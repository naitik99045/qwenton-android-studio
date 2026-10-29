package com.qwenton.studio.apex.terminal

import android.content.Context
import android.util.Log
import com.qwenton.studio.apex.data.model.ServerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.regex.Pattern

/**
 * High-performance, crash-proof native Linux shell execution bridge for Qwenton Studio Apex.
 * Communicates directly with the Android device's `/system/bin/sh` shell process via standard UNIX
 * anonymous pipes, completely eliminating fragile third-party PTY JNI linkage and SELinux devpts restrictions.
 */
class QwentonTerminalBridge private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var nativeProcess: Process? = null
    private var nativeProcessWriter: BufferedWriter? = null

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
     * Initializes the resilient native Android Linux `/system/bin/sh` process.
     */
    fun initializeSession() {
        startNativeShellEngine()
    }

    private fun startNativeShellEngine() {
        scope.launch(Dispatchers.IO) {
            try {
                if (nativeProcess?.isAlive == true) {
                    return@launch
                }

                val workingDir = appContext.filesDir.absolutePath
                val logsDir = File(appContext.filesDir, "logs")
                if (!logsDir.exists()) logsDir.mkdirs()

                val modelsDir = File(appContext.filesDir, "models")
                if (!modelsDir.exists()) modelsDir.mkdirs()

                val agentsDir = File(appContext.filesDir, "agents")
                if (!agentsDir.exists()) agentsDir.mkdirs()

                val nativeLibDir = appContext.applicationInfo.nativeLibraryDir

                val pb = ProcessBuilder("/system/bin/sh")
                    .directory(File(workingDir))
                    .redirectErrorStream(true)

                val env = pb.environment()
                env["HOME"] = workingDir
                env["TMPDIR"] = logsDir.absolutePath
                env["PATH"] = "/system/bin:/system/xbin:$nativeLibDir"
                env["LD_LIBRARY_PATH"] = nativeLibDir

                val process = pb.start()
                nativeProcess = process
                nativeProcessWriter = BufferedWriter(OutputStreamWriter(process.outputStream, Charsets.UTF_8))

                appendTranscript("==> Qwenton Native Linux Shell Engine activated (/system/bin/sh).\n")
                appendTranscript("==> Environment: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})\n\n")

                val reader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { outputLine ->
                        processOutputChunk("$outputLine\n")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Native shell engine exception: ${t.message}", t)
                appendTranscript("==> Engine Error: ${t.localizedMessage}\n")
            }
        }
    }

    /**
     * Dispatches a deterministic shell command directly into the running shell process.
     */
    fun dispatchCommand(command: String) {
        val cleanCmd = command.trim() + "\n"
        scope.launch(Dispatchers.IO) {
            try {
                if (nativeProcessWriter == null || nativeProcess?.isAlive != true) {
                    startNativeShellEngine()
                }
                nativeProcessWriter?.write(cleanCmd)
                nativeProcessWriter?.flush()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed writing command to shell process", t)
                appendTranscript("==> Error dispatching command: ${t.localizedMessage}\n")
            }
        }
    }

    /**
     * Appends content to the observable transcript and limits memory consumption.
     */
    private fun appendTranscript(text: String) {
        scope.launch(Dispatchers.Main) {
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
     * Called whenever stdout produces text in the shell session.
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
