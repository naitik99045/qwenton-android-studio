package com.qwenton.studio.apex.terminal

import android.content.Context
import android.util.Log
import com.qwenton.studio.apex.data.model.ServerStatus
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.regex.Pattern

class QwentonTerminalBridge private constructor(private val appContext: Context) : TerminalSessionClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var terminalSession: TerminalSession? = null
    private var nativeProcess: Process? = null
    private var nativeProcessWriter: java.io.BufferedWriter? = null

    // Reactive Compose States
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _latestLogLine = MutableStateFlow("● System Ready")
    val latestLogLine: StateFlow<String> = _latestLogLine.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus.STOPPED)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _localBaseUrl = MutableStateFlow("http://127.0.0.1:8080/v1")
    val localBaseUrl: StateFlow<String> = _localBaseUrl.asStateFlow()

    private val _publicTunnelUrl = MutableStateFlow("")
    val publicTunnelUrl: StateFlow<String> = _publicTunnelUrl.asStateFlow()

    private val _tokenSpeed = MutableStateFlow(0.0f)
    val tokenSpeed: StateFlow<Float> = _tokenSpeed.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Compiled Regex Patterns for Real-Time Stdout Parsing
    private val curlProgressPattern = Pattern.compile("(\\d{1,3})\\.\\d+%|(\\d{1,3})%")
    private val cloudflareUrlPattern = Pattern.compile("https://[a-zA-Z0-9-]+\\.trycloudflare\\.com")
    private val tokenSpeedPattern = Pattern.compile("([0-9]+\\.[0-9]+)\\s*t/s|tokens per second:\\s*([0-9.]+)|eval time = .*?([0-9.]+)\\s*t/s")

    init {
        initializeSession()
    }

    /**
     * Initializes the underlying Termux TerminalSession or transparently falls back to native Linux shell process.
     */
    fun initializeSession() {
        try {
            val workingDir = appContext.filesDir.absolutePath
            val logsDir = File(appContext.filesDir, "logs")
            if (!logsDir.exists()) logsDir.mkdirs()

            val nativeLibDir = appContext.applicationInfo.nativeLibraryDir
            val shell = "/system/bin/sh"
            val env = arrayOf(
                "HOME=$workingDir",
                "TMPDIR=${logsDir.absolutePath}",
                "PATH=/system/bin:/system/xbin:$nativeLibDir",
                "LD_LIBRARY_PATH=$nativeLibDir"
            )

            terminalSession = TerminalSession(
                shell,
                workingDir,
                arrayOf("-l"),
                env,
                2000,
                this
            )
            appendTranscript("==> Qwenton Terminal PTY session created.\n")
        } catch (t: Throwable) {
            // Safely catch UnsatisfiedLinkError, NoClassDefFoundError, or any runtime linkage failure
            Log.w(TAG, "Termux PTY JNI not available on device (${t.message}), activating Native Linux Shell engine", t)
            terminalSession = null
            startNativeShellFallback()
            appendTranscript("==> Native Android Linux shell engine activated.\n")
        }
    }

    private fun startNativeShellFallback() {
        scope.launch(Dispatchers.IO) {
            try {
                val workingDir = appContext.filesDir.absolutePath
                val nativeLibDir = appContext.applicationInfo.nativeLibraryDir
                val pb = ProcessBuilder("/system/bin/sh")
                    .directory(File(workingDir))
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = workingDir
                env["TMPDIR"] = "$workingDir/logs"
                env["PATH"] = "/system/bin:/system/xbin:$nativeLibDir"
                env["LD_LIBRARY_PATH"] = nativeLibDir

                val process = pb.start()
                nativeProcess = process
                nativeProcessWriter = process.outputStream.bufferedWriter()

                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { outputLine ->
                        processOutputChunk("$outputLine\n")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in native shell fallback", t)
            }
        }
    }

    fun getTerminalSession(): TerminalSession? = terminalSession

    /**
     * Dispatches a deterministic shell command directly into the PTY session or native Linux shell process.
     */
    fun dispatchCommand(command: String) {
        val cleanCmd = command.trim() + "\n"
        terminalSession?.let { session ->
            try {
                val bytes = cleanCmd.toByteArray(Charsets.UTF_8)
                session.write(bytes, 0, bytes.size)
                return
            } catch (t: Throwable) {
                Log.w(TAG, "Failed writing to TerminalSession, falling back to native shell", t)
            }
        }

        // Native Linux shell fallback execution
        scope.launch(Dispatchers.IO) {
            try {
                if (nativeProcessWriter == null || nativeProcess?.isAlive == false) {
                    startNativeShellFallback()
                }
                nativeProcessWriter?.write(cleanCmd)
                nativeProcessWriter?.flush()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed writing to native shell fallback", t)
                appendTranscript("==> Error executing command: ${t.localizedMessage}\n")
            }
        }
    }

    /**
     * Appends content to the observable transcript and limits memory consumption.
     */
    private fun appendTranscript(text: String) {
        scope.launch {
            val current = _transcript.value
            val maxLen = 50000
            val combined = current + text
            _transcript.value = if (combined.length > maxLen) {
                combined.substring(combined.length - maxLen)
            } else {
                combined
            }
        }
    }

    /**
     * Clears the terminal transcript.
     */
    fun clearTerminal() {
        _transcript.value = ""
        _latestLogLine.value = "● Terminal Cleared"
    }

    /**
     * Called whenever stdout produces text in the Termux session.
     */
    fun processOutputChunk(chunk: String) {
        appendTranscript(chunk)

        val lines = chunk.split("\r\n", "\n", "\r")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            _latestLogLine.value = trimmed

            // 1. Check readiness & lifecycle tokens
            when {
                trimmed.contains(ShellScriptFactory.TOKEN_INIT_COMPLETE) -> {
                    _isInitialized.value = true
                }
                trimmed.contains(ShellScriptFactory.TOKEN_READY) -> {
                    _serverStatus.value = ServerStatus.RUNNING
                }
                trimmed.contains(ShellScriptFactory.TOKEN_SERVER_STOPPED) -> {
                    _serverStatus.value = ServerStatus.STOPPED
                    _publicTunnelUrl.value = ""
                }
                trimmed.contains(ShellScriptFactory.TOKEN_ERROR) -> {
                    _serverStatus.value = ServerStatus.ERROR
                }
                trimmed.contains(ShellScriptFactory.TOKEN_DOWNLOAD_DONE) -> {
                    _downloadProgress.value = 100
                }
            }

            // 2. Parse curl progress percentage
            val curlMatcher = curlProgressPattern.matcher(trimmed)
            if (curlMatcher.find()) {
                val group = curlMatcher.group(1) ?: curlMatcher.group(2)
                group?.toIntOrNull()?.let { percent ->
                    _downloadProgress.value = percent.coerceIn(0, 100)
                }
            }

            // 3. Parse Cloudflare Quick Tunnel URL
            val tunnelMatcher = cloudflareUrlPattern.matcher(trimmed)
            if (tunnelMatcher.find()) {
                val url = tunnelMatcher.group(0)
                if (!url.isNullOrBlank()) {
                    _publicTunnelUrl.value = "$url/v1"
                    _serverStatus.value = ServerStatus.RUNNING
                }
            }

            // 4. Parse token speed (t/s)
            val speedMatcher = tokenSpeedPattern.matcher(trimmed)
            if (speedMatcher.find()) {
                val speedStr = speedMatcher.group(1) ?: speedMatcher.group(2) ?: speedMatcher.group(3)
                speedStr?.toFloatOrNull()?.let { speed ->
                    _tokenSpeed.value = speed
                }
            }
        }
    }

    // TerminalSessionClient Implementation Callbacks
    override fun onTextChanged(changedSession: TerminalSession) {
        try {
            val emulator = changedSession.emulator
            if (emulator != null) {
                val screen = emulator.screen
                val transcriptText = changedSession.emulator.getScreen().transcriptText
                if (transcriptText.isNotBlank()) {
                    val lastLine = transcriptText.lines().lastOrNull { it.isNotBlank() } ?: ""
                    processOutputChunk(lastLine)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in onTextChanged", e)
        }
    }

    override fun onTitleChanged(changedSession: TerminalSession) {}

    override fun onSessionFinished(finishedSession: TerminalSession) {
        _latestLogLine.value = "● Terminal Session Finished"
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}

    override fun onPasteTextFromClipboard(session: TerminalSession?) {}

    override fun onBell(session: TerminalSession) {}

    override fun onColorsChanged(session: TerminalSession) {}

    override fun onTerminalCursorStateChange(state: Boolean) {}

    override fun getTerminalCursorStyle(): java.lang.Integer? = 0

    override fun logError(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun logWarn(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun logVerbose(tag: String, message: String) {
        Log.v(tag, message)
    }

    override fun logStackTraceWithMessage(tag: String, message: String, e: java.lang.Exception) {
        Log.e(tag, message, e)
    }

    override fun logStackTrace(tag: String, e: java.lang.Exception) {
        Log.e(tag, e.message ?: "Terminal exception", e)
    }

    companion object {
        private const val TAG = "QwentonTerminalBridge"

        @Volatile
        private var INSTANCE: QwentonTerminalBridge? = null

        fun getInstance(context: Context): QwentonTerminalBridge {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QwentonTerminalBridge(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
