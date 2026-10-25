package com.qwenton.studio.apex.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.model.TelemetryState
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.DarkCyanSurface
import com.qwenton.studio.apex.ui.theme.GlassmorphicCard
import com.qwenton.studio.apex.ui.theme.GlassmorphicPill
import com.qwenton.studio.apex.ui.theme.MetallicGold
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSurface
import com.qwenton.studio.apex.ui.theme.StatusAmber
import com.qwenton.studio.apex.ui.theme.StatusGreen
import com.qwenton.studio.apex.ui.theme.StatusRed
import com.qwenton.studio.apex.ui.theme.WarmCream

@Composable
fun TelemetryHeader(
    telemetry: TelemetryState,
    modifier: Modifier = Modifier
) {
    val ramProgress = (telemetry.ramPercent / 100f).coerceIn(0f, 1f)
    val ramColor = when {
        telemetry.ramPercent < 50 -> StatusGreen
        telemetry.ramPercent < 75 -> StatusAmber
        else -> StatusRed
    }

    GlassmorphicCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Header title and Foreground Service status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SYSTEM TELEMETRY",
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                GlassmorphicPill(
                    backgroundColor = if (telemetry.isForegroundServiceRunning) DarkCyanSurface else ObsidianSurface
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.isForegroundServiceRunning) StatusGreen else MutedSlate)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (telemetry.isForegroundServiceRunning) "SERVICE ACTIVE" else "IDLE",
                            color = if (telemetry.isForegroundServiceRunning) StatusGreen else MutedSlate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Live RAM Bar and details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAM Usage",
                    color = WarmCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${telemetry.availableRamMb} MB Free / ${telemetry.totalRamMb} MB (${telemetry.ramPercent}%)",
                    color = MetallicGold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { ramProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ramColor,
                trackColor = ObsidianSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: CPU & Hardware Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassmorphicPill(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "CPU CORES",
                            color = MutedSlate,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${telemetry.cpuCores} Cores (ARM64)",
                            color = WarmCream,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                GlassmorphicPill(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = "ACCELERATION",
                            color = MutedSlate,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (telemetry.isVulkanSupported) "NEON + Vulkan" else "NEON (CPU only)",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
