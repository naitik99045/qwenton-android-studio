package com.qwenton.studio.apex.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.model.ServerStatus
import com.qwenton.studio.apex.terminal.QwentonTerminalBridge
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.CyanBorder
import com.qwenton.studio.apex.ui.theme.DarkCyanSurfaceSolid
import com.qwenton.studio.apex.ui.theme.MetallicGold
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSlate
import com.qwenton.studio.apex.ui.theme.SoftPink
import com.qwenton.studio.apex.ui.theme.StatusGreen
import com.qwenton.studio.apex.ui.theme.StatusRed
import com.qwenton.studio.apex.ui.theme.WarmCream

@Composable
fun CollapsibleTerminalDrawer(
    bridge: QwentonTerminalBridge,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val effectiveExpanded = isExpanded || internalExpanded

    val latestLog by bridge.latestLogLine.collectAsState()
    val serverStatus by bridge.serverStatus.collectAsState()
    val tokenSpeed by bridge.tokenSpeed.collectAsState()
    val transcript by bridge.transcript.collectAsState()
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    var inputCommand by remember { mutableStateOf("") }

    // Auto-scroll to bottom as new log chunks stream in
    LaunchedEffect(transcript) {
        if (transcript.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val statusDotColor = when (serverStatus) {
        ServerStatus.RUNNING -> StatusGreen
        ServerStatus.STARTING -> MetallicGold
        ServerStatus.ERROR -> StatusRed
        ServerStatus.STOPPED -> MutedSlate
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(DarkCyanSurfaceSolid)
            .border(
                width = 1.dp,
                color = CyanBorder,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
    ) {
        // Persistent Mini Dock Header (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    internalExpanded = !effectiveExpanded
                    onToggleExpand()
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Latest log or status text
                Text(
                    text = latestLog,
                    color = WarmCream,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Token Speed Badge if server is actively serving
            if (serverStatus == ServerStatus.RUNNING && tokenSpeed > 0f) {
                Text(
                    text = "⚡ ${String.format("%.1f", tokenSpeed)} t/s",
                    color = MetallicGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Expand/Collapse Chevron
            Icon(
                imageVector = if (effectiveExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = if (effectiveExpanded) "Collapse Terminal" else "Expand Terminal",
                tint = CyanAccent,
                modifier = Modifier.size(20.dp)
            )
        }

        // Expanded Terminal Console View
        AnimatedVisibility(
            visible = effectiveExpanded,
            enter = expandVertically(animationSpec = tween(250)),
            exit = shrinkVertically(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(ObsidianSlate)
            ) {
                // Terminal Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCyanSurfaceSolid)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NATIVE SH CONSOLE",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Terminal Logs", transcript))
                                Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Logs",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { bridge.clearTerminal() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Terminal",
                                tint = SoftPink,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                bridge.dispatchCommand("pkill -9 -f libllama_server.so; pkill -9 -f libcloudflared.so")
                                Toast.makeText(context, "Kill signal sent", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Force Kill Processes",
                                tint = StatusRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Interactive Monospace Text Terminal Stream (Crash-Proof Jetpack Compose)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = transcript.ifEmpty {
                                "==> Qwenton Native Linux Terminal Ready.\n==> Shell: /system/bin/sh (POSIX anonymous pipe)\n==> Tap buttons or enter custom commands below.\n"
                            },
                            color = WarmCream,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        )
                    }
                }

                // Quick Command Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCyanSurfaceSolid)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("uname -a", "ps", "df -h", "ls -lh", "top -n 1").forEach { cmd ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ObsidianSlate)
                                .border(1.dp, CyanBorder, RoundedCornerShape(4.dp))
                                .clickable { bridge.dispatchCommand(cmd) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = cmd,
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Interactive Command Prompt Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCyanSurfaceSolid)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ ",
                        color = CyanAccent,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    BasicTextField(
                        value = inputCommand,
                        onValueChange = { inputCommand = it },
                        singleLine = true,
                        cursorBrush = SolidColor(CyanAccent),
                        textStyle = TextStyle(
                            color = WarmCream,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputCommand.isNotBlank()) {
                                    bridge.dispatchCommand(inputCommand)
                                    inputCommand = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = {
                            if (inputCommand.isNotBlank()) {
                                bridge.dispatchCommand(inputCommand)
                                inputCommand = ""
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Command",
                            tint = SoftPink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
