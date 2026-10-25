package com.qwenton.studio.apex.ui.playground

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.qwenton.studio.apex.data.model.ChatMessage
import com.qwenton.studio.apex.data.model.ServerStatus
import com.qwenton.studio.apex.terminal.QwentonTerminalBridge
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
import com.qwenton.studio.apex.ui.theme.WarmCream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

@Composable
fun PlaygroundScreen(
    bridge: QwentonTerminalBridge,
    apiKeyManager: ApiKeyManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val serverStatus by bridge.serverStatus.collectAsState()
    val tokenSpeed by bridge.tokenSpeed.collectAsState()

    var inputMessage by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "assistant",
                content = "👋 Welcome to Qwenton Local Playground! When the AI server is running, you can test models with streaming responses and formatted code blocks."
            )
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val userText = inputMessage.trim()
        if (userText.isEmpty() || isGenerating) return

        if (serverStatus != ServerStatus.RUNNING) {
            Toast.makeText(context, "Server is not running. Please start the server on the Dashboard.", Toast.LENGTH_SHORT).show()
            return
        }

        messages.add(ChatMessage(role = "user", content = userText))
        inputMessage = ""
        isGenerating = true

        val assistantMessageIndex = messages.size
        messages.add(ChatMessage(role = "assistant", content = "", isStreaming = true))

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .build()

                    val messagesJson = JSONArray()
                    for (msg in messages.take(assistantMessageIndex)) {
                        messagesJson.put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }

                    val payload = JSONObject().apply {
                        put("model", "default")
                        put("messages", messagesJson)
                        put("stream", true)
                        put("temperature", 0.7)
                    }

                    val apiKey = apiKeyManager.getOrCreateApiKey()
                    val request = Request.Builder()
                        .url("http://127.0.0.1:8080/v1/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                messages[assistantMessageIndex] = ChatMessage(
                                    role = "assistant",
                                    content = "⚠️ HTTP Error ${response.code}: ${response.message}"
                                )
                                isGenerating = false
                            }
                            return@withContext
                        }

                        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
                        var line: String?
                        val accumulated = StringBuilder()

                        while (reader.readLine().also { line = it } != null) {
                            val streamLine = line ?: break
                            if (streamLine.startsWith("data: ")) {
                                val data = streamLine.substring(6).trim()
                                if (data == "[DONE]") break

                                try {
                                    val deltaJson = JSONObject(data)
                                    val choices = deltaJson.optJSONArray("choices")
                                    if (choices != null && choices.length() > 0) {
                                        val delta = choices.getJSONObject(0).optJSONObject("delta")
                                        val chunk = delta?.optString("content") ?: ""
                                        accumulated.append(chunk)

                                        withContext(Dispatchers.Main) {
                                            if (assistantMessageIndex < messages.size) {
                                                messages[assistantMessageIndex] = ChatMessage(
                                                    role = "assistant",
                                                    content = accumulated.toString(),
                                                    isStreaming = true
                                                )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Non-JSON SSE event
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            if (assistantMessageIndex < messages.size) {
                                messages[assistantMessageIndex] = ChatMessage(
                                    role = "assistant",
                                    content = accumulated.toString(),
                                    isStreaming = false
                                )
                            }
                            isGenerating = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (assistantMessageIndex < messages.size) {
                        messages[assistantMessageIndex] = ChatMessage(
                            role = "assistant",
                            content = "⚠️ Connection error: ${e.localizedMessage ?: "Failed to reach local server"}"
                        )
                    }
                    isGenerating = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianSlate)
            .padding(16.dp)
    ) {
        // Playground Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LOCAL PLAYGROUND",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (serverStatus == ServerStatus.RUNNING) "Connected to 127.0.0.1:8080" else "Server Offline",
                    color = if (serverStatus == ServerStatus.RUNNING) StatusGreen else MutedSlate,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tokenSpeed > 0f) {
                    GlassmorphicPill(backgroundColor = ObsidianSurface) {
                        Text(
                            text = "⚡ ${String.format("%.1f", tokenSpeed)} t/s",
                            color = MetallicGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                    onClick = {
                        messages.clear()
                        messages.add(
                            ChatMessage(
                                role = "assistant",
                                content = "Chat cleared. Ask anything to test your local model!"
                            )
                        )
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Chat",
                        tint = MutedSlate,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Feed
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { message ->
                ChatMessageItem(message = message)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Input Deck
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 60.dp), // Clearance for terminal dock
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("Ask local AI model...", color = MutedSlate, fontSize = 13.sp) },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SoftPink,
                    unfocusedBorderColor = CyanBorder,
                    focusedTextColor = WarmCream,
                    unfocusedTextColor = WarmCream
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { sendMessage() },
                enabled = !isGenerating && inputMessage.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (!isGenerating && inputMessage.isNotBlank()) SoftPink else DarkCyanSurfaceSolid)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CyanAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputMessage.isNotBlank()) ObsidianSlate else MutedSlate,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
