package com.qwenton.studio.apex.ui.dashboard

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.local.ApiKeyManager
import com.qwenton.studio.apex.data.local.ModelCatalogRepository
import com.qwenton.studio.apex.data.model.GgufModelInfo
import com.qwenton.studio.apex.data.model.ServerStatus
import com.qwenton.studio.apex.data.model.TelemetryState
import com.qwenton.studio.apex.service.GuardrailStatus
import com.qwenton.studio.apex.service.QwentonServerService
import com.qwenton.studio.apex.service.RamGuardrailValidator
import com.qwenton.studio.apex.terminal.QwentonTerminalBridge
import com.qwenton.studio.apex.terminal.ShellScriptFactory
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.CyanBorder
import com.qwenton.studio.apex.ui.theme.DarkCyanSurface
import com.qwenton.studio.apex.ui.theme.GlassmorphicCard
import com.qwenton.studio.apex.ui.theme.GlassmorphicPill
import com.qwenton.studio.apex.ui.theme.MetallicGold
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSurface
import com.qwenton.studio.apex.ui.theme.SoftPink
import com.qwenton.studio.apex.ui.theme.StatusAmber
import com.qwenton.studio.apex.ui.theme.StatusGreen
import com.qwenton.studio.apex.ui.theme.StatusRed
import com.qwenton.studio.apex.ui.theme.WarmCream
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDashboardScreen(
    bridge: QwentonTerminalBridge,
    repository: ModelCatalogRepository,
    apiKeyManager: ApiKeyManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Reactive states
    val installedModels by repository.installedModels.collectAsState()
    val serverStatus by bridge.serverStatus.collectAsState()
    val localBaseUrl by bridge.localBaseUrl.collectAsState()
    val publicTunnelUrl by bridge.publicTunnelUrl.collectAsState()
    val isInitialized by bridge.isInitialized.collectAsState()

    var currentApiKey by remember { mutableStateOf(apiKeyManager.getOrCreateApiKey()) }
    var selectedModel by remember { mutableStateOf<GgufModelInfo?>(null) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    var contextSize by remember { mutableIntStateOf(4096) }
    var gpuLayers by remember { mutableIntStateOf(0) }

    val ramValidator = remember { RamGuardrailValidator(context) }
    val (availRamMb, totalRamMb) = remember(serverStatus) { ramValidator.getMemoryInfo() }
    val ramPercent = if (totalRamMb > 0) (((totalRamMb - availRamMb).toFloat() / totalRamMb) * 100).toInt() else 0

    val telemetry = TelemetryState(
        availableRamMb = availRamMb,
        totalRamMb = totalRamMb,
        ramPercent = ramPercent,
        cpuCores = Runtime.getRuntime().availableProcessors(),
        isVulkanSupported = true,
        isForegroundServiceRunning = serverStatus == ServerStatus.RUNNING,
        tokenSpeed = 0f,
        activeModelName = selectedModel?.name
    )

    // Automatically select first installed model if available
    LaunchedEffect(installedModels) {
        if (selectedModel == null && installedModels.isNotEmpty()) {
            selectedModel = installedModels.first()
        }
    }

    // Pre-flight RAM evaluation
    val guardrailDecision = remember(selectedModel, contextSize, gpuLayers) {
        selectedModel?.localPath?.let { path ->
            ramValidator.evaluateModelLoad(path, contextSize, gpuLayers)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSlate)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hardware & RAM Telemetry Header
        TelemetryHeader(telemetry = telemetry)

        // 2. Active Endpoint & Key Card
        EndpointCard(
            localUrl = localBaseUrl,
            publicTunnelUrl = publicTunnelUrl,
            apiKey = currentApiKey,
            onRotateApiKey = {
                currentApiKey = apiKeyManager.rotateApiKey()
                Toast.makeText(context, "API Key rotated securely", Toast.LENGTH_SHORT).show()
            }
        )

        // 3. First Launch Bootstrap CTA (if needed)
        if (!isInitialized) {
            GlassmorphicCard(
                borderColor = CyanAccent,
                backgroundColor = DarkCyanSurface
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = "Initialize Studio",
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bootstrap AI Engine",
                            color = WarmCream,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Initialize the ARM64 environment, prepare directory structures, and link native libraries.",
                        color = MutedSlate,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val cmd = ShellScriptFactory.buildInitBootstrapCommand(
                                filesDir = context.filesDir.absolutePath,
                                nativeLibDir = context.applicationInfo.nativeLibraryDir
                            )
                            bridge.dispatchCommand(cmd)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("1-Click Initialize Studio", color = ObsidianSlate, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Server Control Deck Card
        GlassmorphicCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SERVER CONTROL DECK",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Model Selector Dropdown
                Text(
                    text = "Active GGUF Model",
                    color = WarmCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = isModelDropdownExpanded,
                    onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedModel?.name ?: "No models found (Visit Hub to download)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = CyanBorder,
                            focusedTextColor = WarmCream,
                            unfocusedTextColor = WarmCream
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isModelDropdownExpanded,
                        onDismissRequest = { isModelDropdownExpanded = false }
                    ) {
                        if (installedModels.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No installed models. Download one from the Hub.") },
                                onClick = { isModelDropdownExpanded = false }
                            )
                        } else {
                            installedModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.name, color = WarmCream, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${model.quantType} • ${model.formattedSize}",
                                                color = MutedSlate,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedModel = model
                                        isModelDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Guardrail Feedback Badge
                guardrailDecision?.let { decision ->
                    Spacer(modifier = Modifier.height(10.dp))
                    val bannerColor = when (decision.status) {
                        GuardrailStatus.SAFE -> StatusGreen
                        GuardrailStatus.WARNING -> StatusAmber
                        GuardrailStatus.CRITICAL -> StatusRed
                    }
                    GlassmorphicPill(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = ObsidianSurface,
                        borderColor = bannerColor
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Guardrail Status",
                                tint = bannerColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = decision.message,
                                color = bannerColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Context Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Context Length (-c)", color = WarmCream, fontSize = 13.sp)
                    Text("$contextSize tokens", color = MetallicGold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = contextSize.toFloat(),
                    onValueChange = { contextSize = it.toInt() },
                    valueRange = 2048f..16384f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = SoftPink,
                        activeTrackColor = SoftPink,
                        inactiveTrackColor = ObsidianSurface
                    )
                )

                // GPU Offload Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GPU Layers Offloaded (-ngl)", color = WarmCream, fontSize = 13.sp)
                    Text(if (gpuLayers == 0) "CPU Only (0)" else "$gpuLayers layers (Vulkan)", color = CyanAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = gpuLayers.toFloat(),
                    onValueChange = { gpuLayers = it.toInt() },
                    valueRange = 0f..33f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = ObsidianSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start / Stop Hero CTA Button
                val isRunning = serverStatus == ServerStatus.RUNNING
                val isStarting = serverStatus == ServerStatus.STARTING

                Button(
                    onClick = {
                        if (isRunning) {
                            // Stop Server
                            QwentonServerService.stop(context)
                        } else {
                            // Start Server
                            val model = selectedModel
                            if (model == null || model.localPath == null) {
                                Toast.makeText(context, "Please install or select a GGUF model first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val logFile = File(context.filesDir, "logs/llama_server.log")
                            if (!logFile.parentFile.exists()) logFile.parentFile.mkdirs()

                            val cmd = ShellScriptFactory.buildStartServerCommand(
                                nativeLibDir = context.applicationInfo.nativeLibraryDir,
                                modelPath = model.localPath,
                                port = 8080,
                                ctxSize = contextSize,
                                threads = guardrailDecision?.suggestedThreads ?: 4,
                                gpuLayers = gpuLayers,
                                apiKey = currentApiKey,
                                logPath = logFile.absolutePath
                            )
                            bridge.dispatchCommand(cmd)
                            QwentonServerService.start(context, model.name, "http://127.0.0.1:8080/v1")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) StatusRed else SoftPink
                    ),
                    enabled = !isStarting
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isStarting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ObsidianSlate,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Booting llama-server & Tunnel...", color = ObsidianSlate, fontWeight = FontWeight.Bold)
                        } else if (isRunning) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = WarmCream)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop AI Server", color = WarmCream, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = ObsidianSlate)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Local AI Server", color = ObsidianSlate, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(70.dp)) // Extra clearance for terminal mini dock
    }
}
