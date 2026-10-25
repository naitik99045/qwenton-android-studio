package com.qwenton.studio.apex.terminal

import java.io.File

object ShellScriptFactory {

    const val TOKEN_INIT_COMPLETE = "[QWENTON_INIT_COMPLETE]"
    const val TOKEN_DOWNLOAD_DONE = "[QWENTON_DOWNLOAD_DONE]"
    const val TOKEN_READY = "[QWENTON_READY]"
    const val TOKEN_SERVER_STOPPED = "[QWENTON_SERVER_STOPPED]"
    const val TOKEN_ERROR = "[QWENTON_ERROR]"

    /**
     * Sanitizes input strings for safe inclusion in POSIX shell commands.
     * Replaces single quotes with safe concatenation: '\''
     */
    fun escapeShellArg(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }

    /**
     * Builds the studio bootstrapping shell script.
     */
    fun buildInitBootstrapCommand(
        filesDir: String,
        nativeLibDir: String
    ): String {
        val safeFilesDir = escapeShellArg(filesDir)
        val safeNativeDir = escapeShellArg(nativeLibDir)

        return """
            export HOME=$safeFilesDir
            cd $safeFilesDir
            mkdir -p models agents rootfs logs
            
            echo "==> [Qwenton Studio Engine] Initializing environment..."
            echo "Checking native binaries in $safeNativeDir..."
            
            for bin in libllama_server.so libcloudflared.so libproot.so; do
                if [ -f "$safeNativeDir/${'$'}bin" ]; then
                    echo "  [FOUND] ${'$'}bin"
                else
                    echo "  [MISSING] ${'$'}bin (Will require packaging or download)"
                fi
            done
            
            echo "$TOKEN_INIT_COMPLETE"
        """.trimIndent()
    }

    /**
     * Builds the resumable curl download pipeline for GGUF models and optional pip tool setup.
     */
    fun buildDownloadModelCommand(
        url: String,
        destinationFilePath: String,
        pipPackages: List<String> = emptyList(),
        nativeLibDir: String = "",
        filesDir: String = ""
    ): String {
        val safeUrl = escapeShellArg(url)
        val safeDest = escapeShellArg(destinationFilePath)
        val tempDest = escapeShellArg("$destinationFilePath.part")

        // Filter and sanitize pip packages against option injection (e.g. --extra-index-url)
        val validPackagePattern = Regex("^[a-zA-Z0-9_-]+([>=<~]=?[a-zA-Z0-9_.-]+)?$")
        val safePackages = pipPackages.filter { it.matches(validPackagePattern) }

        val pipInstallCommands = if (safePackages.isNotEmpty() && nativeLibDir.isNotBlank() && filesDir.isNotBlank()) {
            val safeNativeDir = escapeShellArg(nativeLibDir)
            val safeFilesDir = escapeShellArg(filesDir)
            val packagesStr = safePackages.joinToString(" ") { escapeShellArg(it) }
            """
            echo "==> Installing verified Python dependencies inside rootfs: $packagesStr"
            if [ -x "$safeNativeDir/libproot.so" ]; then
                $safeNativeDir/libproot.so -0 -r "$safeFilesDir/rootfs" -b /dev -b /proc -b /sys \
                    /usr/bin/pip install --no-cache-dir $packagesStr || echo "Pip install finished with warning"
            fi
            """.trimIndent()
        } else {
            ""
        }

        return """
            echo "==> Downloading GGUF model via resumable curl..."
            echo "Target: $safeDest"
            curl -L -C - --progress-bar --fail $safeUrl -o $tempDest
            if [ $? -eq 0 ]; then
                mv $tempDest $safeDest
                echo "==> Download verified successfully."
                $pipInstallCommands
                echo "$TOKEN_DOWNLOAD_DONE"
            else
                echo "==> Download failed or interrupted."
                echo "$TOKEN_ERROR"
            fi
        """.trimIndent()
    }

    /**
     * Builds the deterministic server startup script:
     * 1. Kills existing instances
     * 2. Launches libllama_server.so in background
     * 3. Polls http://127.0.0.1:$port/health until HTTP 200 is confirmed
     * 4. Spawns libcloudflared.so tunnel
     */
    fun buildStartServerCommand(
        nativeLibDir: String,
        modelPath: String,
        port: Int = 8080,
        ctxSize: Int = 4096,
        threads: Int = 4,
        gpuLayers: Int = 0,
        apiKey: String,
        logPath: String
    ): String {
        val safeNativeDir = escapeShellArg(nativeLibDir)
        val safeModel = escapeShellArg(modelPath)
        val safeKey = escapeShellArg(apiKey)
        val safeLog = escapeShellArg(logPath)

        return """
            echo "==> Stopping any prior server instances..."
            pkill -f libllama_server.so 2>/dev/null || true
            pkill -f libcloudflared.so 2>/dev/null || true
            sleep 1
            
            echo "==> Launching llama.cpp server on 0.0.0.0:$port (ctx: $ctxSize, threads: $threads, ngl: $gpuLayers)..."
            $safeNativeDir/libllama_server.so \
                -m $safeModel \
                --host 0.0.0.0 \
                --port $port \
                -c $ctxSize \
                -t $threads \
                -ngl $gpuLayers \
                --load-mode mmap \
                --api-key $safeKey \
                --metrics > $safeLog 2>&1 &
            
            LLAMA_PID=$!
            echo "llama-server spawned with PID: ${'$'}LLAMA_PID"
            
            # Health check loop (wait up to 45 seconds for model weights to map into memory)
            echo "==> Waiting for local endpoint http://127.0.0.1:$port/health to report OK..."
            SUCCESS=0
            for i in $(seq 1 45); do
                if curl -s -f -H "Authorization: Bearer $safeKey" "http://127.0.0.1:$port/health" > /dev/null 2>&1; then
                    SUCCESS=1
                    break
                fi
                sleep 1
            done
            
            if [ ${'$'}SUCCESS -eq 1 ]; then
                echo "==> Local server is healthy and responding!"
                echo "$TOKEN_READY"
                echo "==> Starting Cloudflare Quick Tunnel for public HTTPS access..."
                $safeNativeDir/libcloudflared.so tunnel --url "http://127.0.0.1:$port"
            else
                echo "==> Server health check timed out. Inspecting log:"
                tail -n 20 $safeLog
                echo "$TOKEN_ERROR"
            fi
        """.trimIndent()
    }

    /**
     * Builds the deterministic server shutdown script.
     */
    fun buildStopServerCommand(): String {
        return """
            echo "==> Stopping Qwenton AI Server & Cloudflare tunnel..."
            pkill -f libllama_server.so 2>/dev/null || true
            pkill -f libcloudflared.so 2>/dev/null || true
            sleep 1
            echo "$TOKEN_SERVER_STOPPED"
        """.trimIndent()
    }
}
