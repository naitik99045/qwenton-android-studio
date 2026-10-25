package com.qwenton.studio.apex.ui.playground

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qwenton.studio.apex.data.model.ChatMessage
import com.qwenton.studio.apex.ui.theme.CodeBlockBackground
import com.qwenton.studio.apex.ui.theme.CyanAccent
import com.qwenton.studio.apex.ui.theme.CyanBorder
import com.qwenton.studio.apex.ui.theme.DarkCyanSurface
import com.qwenton.studio.apex.ui.theme.MetallicGold
import com.qwenton.studio.apex.ui.theme.MutedSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSlate
import com.qwenton.studio.apex.ui.theme.ObsidianSurface
import com.qwenton.studio.apex.ui.theme.SoftPink
import com.qwenton.studio.apex.ui.theme.TerminalGreen
import com.qwenton.studio.apex.ui.theme.WarmCream

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val isSystem = message.role == "system"
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = when {
            isUser -> Arrangement.End
            isSystem -> Arrangement.Center
            else -> Arrangement.Start
        }
    ) {
        val bubbleShape = when {
            isUser -> RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            isSystem -> RoundedCornerShape(8.dp)
            else -> RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        }

        val bubbleBackground = when {
            isUser -> DarkCyanSurface
            isSystem -> ObsidianSurface
            else -> ObsidianSurface
        }

        val bubbleBorder = when {
            isUser -> SoftPink.copy(alpha = 0.5f)
            isSystem -> MutedSlate.copy(alpha = 0.3f)
            else -> CyanBorder
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(if (isSystem) 0.95f else 0.88f)
                .clip(bubbleShape)
                .background(bubbleBackground)
                .border(width = 1.dp, color = bubbleBorder, shape = bubbleShape)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (message.role) {
                            "user" -> "YOU"
                            "system" -> "SYSTEM INSTRUCTIONS"
                            else -> "ASSISTANT"
                        },
                        color = when (message.role) {
                            "user" -> SoftPink
                            "system" -> MetallicGold
                            else -> CyanAccent
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Chat Message", message.content))
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Message",
                            tint = MutedSlate,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Parse content into regular text and code blocks
                RenderMessageContent(content = message.content)
            }
        }
    }
}

@Composable
private fun RenderMessageContent(content: String) {
    val context = LocalContext.current
    val parts = content.split("```")

    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) {
            // Code block
            val lines = part.lines()
            val language = lines.firstOrNull()?.trim() ?: "code"
            val code = lines.drop(1).joinToString("\n").trim()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CodeBlockBackground)
                    .border(1.dp, CyanBorder, RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ObsidianSlate)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.ifBlank { "snippet" }.uppercase(),
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Code", code))
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = CyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Text(
                    text = code,
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            // Plain text
            if (part.isNotBlank()) {
                Text(
                    text = part.trim(),
                    color = WarmCream,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}
