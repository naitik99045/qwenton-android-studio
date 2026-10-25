package com.qwenton.studio.apex.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.model.GgufModelInfo
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.DarkCyanSurface
import com.qwenton.studio.apex.ui.theme.GlassmorphicCard
import com.qwenton.studio.apex.ui.theme.GlassmorphicPill
import com.qwenton.studio.apex.ui.theme.MetallicGold
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSurface
import com.qwenton.studio.apex.ui.theme.SoftPink
import com.qwenton.studio.apex.ui.theme.StatusGreen
import com.qwenton.studio.apex.ui.theme.WarmCream

@Composable
fun ModelDownloadCard(
    model: GgufModelInfo,
    isDownloading: Boolean,
    downloadProgress: Int,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        color = WarmCream,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = model.repoId,
                        color = MutedSlate,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                GlassmorphicPill(backgroundColor = ObsidianSurface) {
                    Text(
                        text = model.quantType,
                        color = MetallicGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (model.description.isNotBlank()) {
                Text(
                    text = model.description,
                    color = MutedSlate,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Size: ${model.formattedSize}",
                    color = WarmCream,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (model.isInstalled) {
                    GlassmorphicPill(
                        backgroundColor = DarkCyanSurface,
                        borderColor = StatusGreen
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Installed",
                                tint = StatusGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "INSTALLED",
                                color = StatusGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isDownloading) {
                    Text(
                        text = "$downloadProgress%",
                        color = SoftPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Button(
                        onClick = onDownloadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SoftPink),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = ObsidianSlate,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Download",
                                color = ObsidianSlate,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (downloadProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SoftPink,
                    trackColor = ObsidianSurface
                )
            }
        }
    }
}
