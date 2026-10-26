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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import com.termux.terminal.TerminalSession

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
                // Status dot
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

            // Token Speed Badge if active
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
                    .height(340.dp)
                    .background(ObsidianSlate)
            ) {
                // Terminal Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCyanSurfaceSolid)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EMBEDDED PTY CONSOLE",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row {
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

                // Native Termux TerminalView or Safe Compose Fallback Stream
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(6.dp)
                ) {
                    var nativeViewFailed by remember { mutableStateOf(false) }
                    val session = bridge.getTerminalSession()

                    if (session != null && !nativeViewFailed) {
                        AndroidView(
                            factory = { ctx ->
                                try {
                                    TerminalView(ctx, null).apply {
                                        setTerminalViewClient(ApexTerminalViewClient())
                                        setTextSize(26)
                                        attachSession(session)
                                    }
                                } catch (t: Throwable) {
                                    android.util.Log.w("CollapsibleDrawer", "TerminalView instantiation fallback", t)
                                    nativeViewFailed = true
                                    android.widget.TextView(ctx).apply {
                                        setTextColor(android.graphics.Color.WHITE)
                                        text = transcript
                                    }
                                }
                            },
                            update = { termView ->
                                try {
                                    if (termView is TerminalView && termView.currentSession != session) {
                                        termView.attachSession(session)
                                    }
                                } catch (t: Throwable) {
                                    nativeViewFailed = true
                                }
                            },
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        // Resilient scrollable Compose text console
                        Text(
                            text = transcript.ifEmpty { "==> Qwenton Terminal Ready. No output yet.\n" },
                            color = WarmCream,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .matchParentSize()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

private class ApexTerminalViewClient : TerminalViewClient {
    override fun onScale(scale: Float): Float = scale
    override fun onSingleTapUp(e: android.view.MotionEvent) {}
    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = false
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}
    override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent): Boolean = false
    override fun onLongPress(event: android.view.MotionEvent): Boolean = false
    override fun readControlKey(): Boolean = false
    override fun readAltKey(): Boolean = false
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
    override fun onEmulatorSet() {}
    override fun logError(tag: String, message: String) { android.util.Log.e(tag, message) }
    override fun logWarn(tag: String, message: String) { android.util.Log.w(tag, message) }
    override fun logInfo(tag: String, message: String) { android.util.Log.i(tag, message) }
    override fun logDebug(tag: String, message: String) { android.util.Log.d(tag, message) }
    override fun logVerbose(tag: String, message: String) { android.util.Log.v(tag, message) }
    override fun logStackTraceWithMessage(tag: String, message: String, e: java.lang.Exception) { android.util.Log.e(tag, message, e) }
    override fun logStackTrace(tag: String, e: java.lang.Exception) { android.util.Log.e(tag, e.message ?: "", e) }
}
