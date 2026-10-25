package com.qwenton.studio.apex.ui.components

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.model.ServerStatus
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.CyanBorder
import com.qwenton.studio.apex.ui.theme.DarkCyanSurfaceSolid
import com.qwenton.studio.apex.ui.theme.GlassmorphicPill
import com.qwenton.studio.apex.ui.theme.MetallicGold
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSurface
import com.qwenton.studio.apex.ui.theme.SoftPink
import com.qwenton.studio.apex.ui.theme.StatusGreen
import com.qwenton.studio.apex.ui.theme.StatusRed
import com.qwenton.studio.apex.ui.theme.WarmCream

@Composable
fun StudioTopAppBar(
    serverStatus: ServerStatus,
    onInfoClick: () -> Unit,
    onTerminalToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (serverStatus) {
        ServerStatus.RUNNING -> StatusGreen
        ServerStatus.STARTING -> MetallicGold
        ServerStatus.ERROR -> StatusRed
        ServerStatus.STOPPED -> MutedSlate
    }

    val statusText = when (serverStatus) {
        ServerStatus.RUNNING -> "SERVER ACTIVE"
        ServerStatus.STARTING -> "STARTING..."
        ServerStatus.ERROR -> "ERROR"
        ServerStatus.STOPPED -> "IDLE"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkCyanSurfaceSolid)
            .statusBarsPadding()
            .border(
                width = 1.dp,
                color = CyanBorder,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title with APEX Pill Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "QWENTON",
                    color = WarmCream,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                GlassmorphicPill(
                    backgroundColor = ObsidianSurface,
                    borderColor = SoftPink.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "APEX",
                        color = SoftPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right Actions: Live Server Status Pill & Navigation Action Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Interactive Status Pill
                GlassmorphicPill(
                    backgroundColor = ObsidianSurface,
                    borderColor = statusColor.copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Terminal Toggle Action Button
                IconButton(
                    onClick = onTerminalToggle,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Toggle Terminal",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Info / System Specs Action Button
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "App & System Info",
                        tint = WarmCream,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
