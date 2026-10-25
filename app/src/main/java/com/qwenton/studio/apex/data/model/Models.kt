package com.qwenton.studio.apex.data.model

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

@Serializable
data class GgufModelInfo(
    val id: String,
    val name: String,
    val repoId: String,
    val fileName: String,
    val sizeBytes: Long,
    val quantType: String,
    val isInstalled: Boolean = false,
    val localPath: String? = null,
    val downloadProgress: Int = 0,
    val downloadSpeed: String = "",
    val description: String = ""
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val clamped = digitGroups.coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, clamped.toDouble())
            return String.format(Locale.US, "%.2f %s", value, units[clamped])
        }
    }
}

@Serializable
data class AgentManifest(
    val agentId: String,
    val name: String,
    val description: String,
    val baseModelRepo: String,
    val ggufFileName: String,
    val systemPrompt: String,
    val temperature: Float = 0.7f,
    val contextLength: Int = 4096,
    val pipPackages: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val isInstalled: Boolean = false
)

@Serializable
data class ServerConfig(
    val modelPath: String,
    val modelName: String = "",
    val port: Int = 8080,
    val contextSize: Int = 4096,
    val threads: Int = 4,
    val gpuLayers: Int = 0,
    val apiKey: String,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f
)

@Serializable
data class TelemetryState(
    val availableRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val ramPercent: Int = 0,
    val cpuCores: Int = 8,
    val isVulkanSupported: Boolean = true,
    val isForegroundServiceRunning: Boolean = false,
    val tokenSpeed: Float = 0.0f,
    val activeModelName: String? = null
) {
    val ramSummary: String
        get() = "${availableRamMb} MB / ${totalRamMb} MB ($ramPercent%)"

    val formattedTokenSpeed: String
        get() = String.format(Locale.US, "%.1f t/s", tokenSpeed)
}

@Serializable
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "system", "user", "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatApiMessage>,
    val temperature: Float = 0.7f,
    val stream: Boolean = true
)

@Serializable
data class ChatApiMessage(
    val role: String,
    val content: String
)
