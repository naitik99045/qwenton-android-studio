package com.qwenton.studio.apex.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.model.AgentManifest
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
fun AgentManifestCard(
    agent: AgentManifest,
    onInstallAgent: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Autonomous Agent",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = agent.name,
                        color = WarmCream,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                GlassmorphicPill(backgroundColor = ObsidianSurface) {
                    Text(
                        text = "AGENT",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = agent.description,
                color = MutedSlate,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Required Base Model & Tools
            Text(
                text = "Base Model: ${agent.ggufFileName}",
                color = WarmCream,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            if (agent.pipPackages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Python Packages: ${agent.pipPackages.joinToString(", ")}",
                    color = MetallicGold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onInstallAgent,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (agent.isInstalled) "Agent Ready (Deploy)" else "Install Agent & Dependencies",
                    color = ObsidianSlate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
