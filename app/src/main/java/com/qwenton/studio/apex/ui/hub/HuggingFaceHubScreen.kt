package com.qwenton.studio.apex.ui.hub

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.api.HuggingFaceClient
import com.qwenton.studio.apex.data.local.ModelCatalogRepository
import com.qwenton.studio.apex.data.model.GgufModelInfo
import com.qwenton.studio.apex.terminal.QwentonTerminalBridge
import com.qwenton.studio.apex.terminal.ShellScriptFactory
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.CyanBorder
import com.qwenton.studio.apex.ui.theme.DarkCyanSurfaceSolid
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSlate
import com.qwenton.studio.apex.ui.theme.SoftPink
import com.qwenton.studio.apex.ui.theme.WarmCream
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HuggingFaceHubScreen(
    bridge: QwentonTerminalBridge,
    repository: ModelCatalogRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hfClient = remember { HuggingFaceClient() }

    val installedModels by repository.installedModels.collectAsState()
    val installedAgents by repository.installedAgents.collectAsState()
    val downloadProgress by bridge.downloadProgress.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<GgufModelInfo>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentlyDownloadingModelId by remember { mutableStateOf<String?>(null) }

    fun startDownload(model: GgufModelInfo) {
        val modelsDir = repository.getModelsDirectory()

        // Strict basename extraction and character sanitization to prevent path traversal
        val rawName = File(model.fileName).name
        val safeName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destFile = File(modelsDir, safeName).canonicalFile

        if (!destFile.path.startsWith(modelsDir.canonicalPath)) {
            Toast.makeText(context, "Security error: Invalid destination path", Toast.LENGTH_SHORT).show()
            return
        }

        val downloadUrl = hfClient.getDirectDownloadUrl(model.repoId, safeName)

        currentlyDownloadingModelId = model.id
        val cmd = ShellScriptFactory.buildDownloadModelCommand(
            url = downloadUrl,
            destinationFilePath = destFile.absolutePath,
            pipPackages = emptyList(),
            nativeLibDir = context.applicationInfo.nativeLibraryDir,
            filesDir = context.filesDir.absolutePath
        )
        bridge.dispatchCommand(cmd)
        Toast.makeText(context, "Download started for ${model.name}", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSlate)
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "HUGGING FACE & AGENT HUB",
            color = MutedSlate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.length >= 3) {
                    isSearching = true
                    selectedTab = 1
                    scope.launch {
                        searchResults = hfClient.searchModels(it)
                        isSearching = false
                    }
                }
            },
            placeholder = { Text("Search Hugging Face GGUF models...", color = MutedSlate) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanAccent)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MutedSlate)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = CyanBorder,
                focusedTextColor = WarmCream,
                unfocusedTextColor = WarmCream
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCyanSurfaceSolid,
            contentColor = WarmCream,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SoftPink
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Curated Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("HF Search", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Agents", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Curated Mobile Presets
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(hfClient.curatedPresets) { preset ->
                        val isInstalled = installedModels.any { it.fileName.equals(preset.fileName, ignoreCase = true) }
                        val isDownloading = currentlyDownloadingModelId == preset.id && downloadProgress < 100

                        ModelDownloadCard(
                            model = preset.copy(isInstalled = isInstalled),
                            isDownloading = isDownloading,
                            downloadProgress = if (isDownloading) downloadProgress else 0,
                            onDownloadClick = { startDownload(preset) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
            1 -> {
                // HF Search Results
                if (isSearching) {
                    ModelSkeletonList()
                } else if (searchResults.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Type a search term above" else "No matching GGUF models found",
                            color = MutedSlate,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { model ->
                            val isInstalled = installedModels.any { it.fileName.equals(model.fileName, ignoreCase = true) }
                            val isDownloading = currentlyDownloadingModelId == model.id && downloadProgress < 100

                            ModelDownloadCard(
                                model = model.copy(isInstalled = isInstalled),
                                isDownloading = isDownloading,
                                downloadProgress = if (isDownloading) downloadProgress else 0,
                                onDownloadClick = { startDownload(model) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
            2 -> {
                // Custom Agent Store
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (installedAgents.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No custom agents installed.",
                                    color = WarmCream,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add agent.json manifests to filesDir/agents/ to define autonomous tool-calling agents.",
                                    color = MutedSlate,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        items(installedAgents) { agent ->
                            AgentManifestCard(
                                agent = agent,
                                onInstallAgent = {
                                    Toast.makeText(context, "Deploying ${agent.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelSkeletonList() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCyanSurfaceSolid)
                    .border(1.dp, CyanBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianSlate.copy(alpha = 0.6f))
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianSlate.copy(alpha = 0.4f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanAccent.copy(alpha = 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftPink.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}
