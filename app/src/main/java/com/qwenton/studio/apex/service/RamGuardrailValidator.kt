package com.qwenton.studio.apex.service

import android.app.ActivityManager
import android.content.Context
import java.io.File

enum class GuardrailStatus {
    SAFE,
    WARNING,
    CRITICAL
}

data class GuardrailDecision(
    val status: GuardrailStatus,
    val suggestedContextSize: Int,
    val suggestedThreads: Int,
    val enableMmap: Boolean,
    val estimatedRamUsageMb: Long,
    val availableRamMb: Long,
    val totalRamMb: Long,
    val message: String
)

class RamGuardrailValidator(private val context: Context) {

    private val activityManager: ActivityManager? =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    /**
     * Reads current memory metrics from ActivityManager.MemoryInfo
     */
    fun getMemoryInfo(): Pair<Long, Long> {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val availMb = memoryInfo.availMem / (1024 * 1024)
        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        return Pair(availMb, totalMb)
    }

    /**
     * Evaluates whether a GGUF model file can safely load without triggering Android Low Memory Killer (LMK).
     */
    fun evaluateModelLoad(
        modelFilePath: String,
        requestedContextSize: Int = 4096,
        requestedGpuLayers: Int = 0
    ): GuardrailDecision {
        val (availMb, totalMb) = getMemoryInfo()
        val file = File(modelFilePath)
        val modelSizeMb = if (file.exists()) file.length() / (1024 * 1024) else 2048L

        // Estimate KV Cache RAM requirement: ~0.5MB per 1024 tokens for 3B/7B models
        val kvCacheMb = (requestedContextSize / 1024) * 128L
        val estimatedTotalMb = modelSizeMb + kvCacheMb + 300L // 300MB buffer for llama.cpp runtime

        val maxSafeAvailMb = (availMb * 0.65).toLong()
        val totalCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val safeThreads = when {
            totalCores >= 8 -> 4 // Keep 4 cores free for Android UI and Cloudflare PTY
            totalCores >= 4 -> 3
            else -> 2
        }

        return when {
            estimatedTotalMb > availMb -> {
                GuardrailDecision(
                    status = GuardrailStatus.CRITICAL,
                    suggestedContextSize = 2048,
                    suggestedThreads = safeThreads,
                    enableMmap = true,
                    estimatedRamUsageMb = estimatedTotalMb,
                    availableRamMb = availMb,
                    totalRamMb = totalMb,
                    message = "Critical: Model requires ~${estimatedTotalMb} MB, but only ${availMb} MB RAM is available. Execution may trigger Android OOM crash."
                )
            }
            estimatedTotalMb > maxSafeAvailMb -> {
                GuardrailDecision(
                    status = GuardrailStatus.WARNING,
                    suggestedContextSize = requestedContextSize.coerceAtMost(2048),
                    suggestedThreads = safeThreads,
                    enableMmap = true,
                    estimatedRamUsageMb = estimatedTotalMb,
                    availableRamMb = availMb,
                    totalRamMb = totalMb,
                    message = "Warning: Model will consume ~${estimatedTotalMb} MB (>65% of available RAM). Auto-tuning context length to 2048 and enabling mmap."
                )
            }
            else -> {
                GuardrailDecision(
                    status = GuardrailStatus.SAFE,
                    suggestedContextSize = requestedContextSize,
                    suggestedThreads = safeThreads,
                    enableMmap = true,
                    estimatedRamUsageMb = estimatedTotalMb,
                    availableRamMb = availMb,
                    totalRamMb = totalMb,
                    message = "Hardware Check Passed: Model safely fits within available memory (${estimatedTotalMb} MB estimated vs ${availMb} MB free)."
                )
            }
        }
    }
}
