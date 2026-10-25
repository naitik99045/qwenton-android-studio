package com.qwenton.studio.apex.data.local

import android.content.Context
import android.util.Log
import com.qwenton.studio.apex.data.model.AgentManifest
import com.qwenton.studio.apex.data.model.GgufModelInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class ModelCatalogRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _installedModels = MutableStateFlow<List<GgufModelInfo>>(emptyList())
    val installedModels: StateFlow<List<GgufModelInfo>> = _installedModels.asStateFlow()

    private val _installedAgents = MutableStateFlow<List<AgentManifest>>(emptyList())
    val installedAgents: StateFlow<List<AgentManifest>> = _installedAgents.asStateFlow()

    init {
        ensureDirectoriesExist()
    }

    fun getModelsDirectory(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAgentsDirectory(): File {
        val dir = File(context.filesDir, "agents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun ensureDirectoriesExist() {
        getModelsDirectory()
        getAgentsDirectory()
    }

    /**
     * Scans local filesDir/models/ and filesDir/agents/ to refresh catalog StateFlows.
     */
    suspend fun refreshCatalog() = withContext(ioDispatcher) {
        try {
            val modelsDir = getModelsDirectory()
            val modelFiles = modelsDir.listFiles { file -> file.isFile && file.name.endsWith(".gguf", ignoreCase = true) }
            val models = modelFiles?.map { file ->
                val quant = extractQuantType(file.name)
                GgufModelInfo(
                    id = file.name,
                    name = file.nameWithoutExtension.replace("-", " "),
                    repoId = "local",
                    fileName = file.name,
                    sizeBytes = file.length(),
                    quantType = quant,
                    isInstalled = true,
                    localPath = file.absolutePath
                )
            } ?: emptyList()

            _installedModels.value = models

            val agentsDir = getAgentsDirectory()
            val agentFiles = agentsDir.listFiles { file -> file.isFile && file.name.endsWith(".json", ignoreCase = true) }
            val agents = mutableListOf<AgentManifest>()
            agentFiles?.forEach { file ->
                try {
                    val content = file.readText()
                    val agent = json.decodeFromString<AgentManifest>(content)
                    agents.add(agent.copy(isInstalled = true))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse agent manifest at ${file.name}", e)
                }
            }
            _installedAgents.value = agents

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing catalog", e)
        }
    }

    suspend fun saveAgentManifest(manifest: AgentManifest) = withContext(ioDispatcher) {
        try {
            // Sanitize agentId to strictly alphanumeric, dashes, and underscores
            val safeId = manifest.agentId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64)
            val agentsDir = getAgentsDirectory()
            val file = File(agentsDir, "$safeId.json").canonicalFile

            // Defensive boundary check to ensure file stays inside agents directory
            if (!file.path.startsWith(agentsDir.canonicalPath)) {
                Log.e(TAG, "Security check failed: Agent path escapes agents directory: ${file.path}")
                return@withContext
            }

            val serialized = json.encodeToString(AgentManifest.serializer(), manifest.copy(agentId = safeId))
            file.writeText(serialized)
            refreshCatalog()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save agent manifest: ${manifest.agentId}", e)
        }
    }

    suspend fun deleteModel(filePath: String): Boolean = withContext(ioDispatcher) {
        try {
            val modelsDir = getModelsDirectory()
            val file = File(filePath).canonicalFile

            // Strict path containment check: file MUST reside strictly within modelsDir
            if (!file.path.startsWith(modelsDir.canonicalPath) || file == modelsDir) {
                Log.e(TAG, "Security check failed: Path escapes models directory: $filePath")
                return@withContext false
            }

            val success = file.delete()
            refreshCatalog()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete model file: $filePath", e)
            false
        }
    }

    private fun extractQuantType(fileName: String): String {
        val uppercase = fileName.uppercase()
        val quants = listOf("Q4_K_M", "Q4_0", "Q4_K_S", "Q8_0", "Q5_K_M", "Q6_K", "IQ4_XS", "IQ3_M", "F16")
        return quants.firstOrNull { uppercase.contains(it) } ?: "GGUF"
    }

    companion object {
        private const val TAG = "ModelCatalogRepository"
    }
}
