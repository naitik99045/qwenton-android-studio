package com.qwenton.studio.apex.data.api

import android.util.Log
import com.qwenton.studio.apex.data.model.GgufModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class HfModelSummary(
    val id: String,
    val author: String? = null,
    val downloads: Int = 0,
    val likes: Int = 0
)

class HuggingFaceClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Curated list of high-performance mobile-friendly GGUF models.
     */
    val curatedPresets: List<GgufModelInfo> = listOf(
        GgufModelInfo(
            id = "qwen2.5-coder-3b-q4",
            name = "Qwen 2.5 Coder 3B Instruct",
            repoId = "Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
            fileName = "qwen2.5-coder-3b-instruct-q4_k_m.gguf",
            sizeBytes = 2_050_000_000L,
            quantType = "Q4_K_M",
            description = "High-accuracy coding assistant optimized for mobile devices with NEON/Vulkan offloading."
        ),
        GgufModelInfo(
            id = "llama-3.2-3b-q4",
            name = "Llama 3.2 3B Instruct",
            repoId = "bartowski/Llama-3.2-3B-Instruct-GGUF",
            fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            sizeBytes = 2_150_000_000L,
            quantType = "Q4_K_M",
            description = "Meta's flagship edge model with excellent reasoning and low memory footprint."
        ),
        GgufModelInfo(
            id = "deepseek-r1-1.5b-q4",
            name = "DeepSeek R1 Distill Qwen 1.5B",
            repoId = "bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF",
            fileName = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            sizeBytes = 1_120_000_000L,
            quantType = "Q4_K_M",
            description = "Compact reasoning model with reasoning chain outputs, ideal for phones with <4GB RAM."
        ),
        GgufModelInfo(
            id = "gemma-3-4b-q4",
            name = "Gemma 3 4B IT",
            repoId = "ggml-org/gemma-3-4b-it-GGUF",
            fileName = "gemma-3-4b-it-Q4_0.gguf",
            sizeBytes = 2_600_000_000L,
            quantType = "Q4_0",
            description = "Google's lightweight state-of-the-art open model with multimodal & reasoning capabilities."
        )
    )

    /**
     * Searches Hugging Face models by query keyword.
     */
    suspend fun searchModels(query: String): List<GgufModelInfo> = withContext(Dispatchers.IO) {
        val sanitizedQuery = query.trim().replace(Regex("[^a-zA-Z0-9_-]"), "")
        val url = "https://huggingface.co/api/models?filter=gguf&search=$sanitizedQuery&limit=15"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "QwentonStudioApex/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val summaries = json.decodeFromString<List<HfModelSummary>>(body)

                summaries.map { summary ->
                    val defaultFileName = "${summary.id.substringAfterLast("/")}-Q4_K_M.gguf"
                    GgufModelInfo(
                        id = summary.id,
                        name = summary.id.substringAfterLast("/"),
                        repoId = summary.id,
                        fileName = defaultFileName,
                        sizeBytes = 2_100_000_000L, // Estimated baseline
                        quantType = "Q4_K_M",
                        description = "Hugging Face model by ${summary.author ?: "community"}. Downloads: ${summary.downloads}, Likes: ${summary.likes}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query Hugging Face API", e)
            emptyList()
        }
    }

    /**
     * Resolves the direct download URL for a GGUF file hosted on Hugging Face.
     * Sanitizes inputs to prevent URL injection and path manipulation.
     */
    fun getDirectDownloadUrl(repoId: String, fileName: String): String {
        val safeRepo = repoId.trim().replace(Regex("[^a-zA-Z0-9_.-/]"), "")
        val safeFileName = File(fileName).name.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        return "https://huggingface.co/$safeRepo/resolve/main/$safeFileName?download=true"
    }

    companion object {
        private const val TAG = "HuggingFaceClient"
    }
}
