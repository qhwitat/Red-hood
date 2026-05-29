package com.example.network

import android.util.Log
import com.example.data.ApiKeyManager
import com.example.data.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

class AIApiService(private val apiKeyManager: ApiKeyManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "AIApiService"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Sends a chat completions request and streams text tokens as a Flow.
     */
    fun streamChat(
        provider: String,
        modelName: String,
        systemInstruction: String,
        history: List<MessageEntity>,
        maxTokens: Int,
        temperature: Float,
        memoryType: String,
        fixedWindowLimit: Int
    ): Flow<String> = kotlinx.coroutines.flow.channelFlow {
        // Retrieve appropriate API Key
        val apiKey = when (provider.lowercase()) {
            "groq" -> apiKeyManager.getGroqKey()
            "openrouter" -> apiKeyManager.getOpenRouterKey()
            "gemini" -> apiKeyManager.getGeminiKey()
            "custom" -> apiKeyManager.getCustomKey()
            else -> ""
        }

        if (apiKey.isEmpty() && provider.lowercase() != "custom") {
            send("Error: API Key is missing for provider '$provider'. Please set it in the Connect panel.")
            return@channelFlow
        }

        // Determine which endpoint and URL to hit
        val url = when (provider.lowercase()) {
            "groq" -> "https://api.groq.com/openai/v1/chat/completions"
            "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "gemini" -> "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions"
            "custom" -> apiKeyManager.getCustomUrl()
            else -> ""
        }

        if (url.isEmpty()) {
            send("Error: Unknown model provider '$provider'")
            return@channelFlow
        }

        // Apply Memory management constraints to history
        val resolvedHistory = when (memoryType) {
            "fixed_window" -> {
                if (history.size > fixedWindowLimit) {
                    history.takeLast(fixedWindowLimit)
                } else {
                    history
                }
            }
            "infinite" -> history
            else -> history
        }

        // Build request body according to OpenAI specifications
        val requestJson = JSONObject()
        requestJson.put("model", modelName)
        requestJson.put("stream", true)
        requestJson.put("max_tokens", maxTokens)
        requestJson.put("temperature", temperature)

        val messagesArray = JSONArray()

        if (systemInstruction.isNotEmpty()) {
            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", systemInstruction)
            messagesArray.put(systemMsg)
        }

        resolvedHistory.forEach { msg ->
            val msgJson = JSONObject()
            val finalRole = when (msg.role.lowercase()) {
                "user" -> "user"
                "assistant", "ai" -> "assistant"
                else -> "user"
            }
            msgJson.put("role", finalRole)
            msgJson.put("content", msg.content)
            messagesArray.put(msgJson)
        }

        requestJson.put("messages", messagesArray)

        // Build Network Request
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))

        requestBuilder.addHeader("Content-Type", "application/json")
        when (provider.lowercase()) {
            "groq", "openrouter" -> {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                if (provider.lowercase() == "openrouter") {
                    requestBuilder.addHeader("HTTP-Referer", "https://ai.studio/build")
                    requestBuilder.addHeader("X-Title", "CyberAI Playground")
                }
            }
            "gemini" -> {
                if (apiKey.startsWith("AIzaSy")) {
                    requestBuilder.url("$url?key=$apiKey")
                } else {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }
            }
            "custom" -> {
                if (apiKey.isNotEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }
            }
        }

        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    send("Error [Code ${response.code}]: ${response.body?.string() ?: "Unknown error"}")
                    return@channelFlow
                }

                val source = response.body?.source() ?: throw IOException("Empty response")
                val reader = BufferedReader(source.inputStream().reader())
                var line: String?

                var lastEmit = System.currentTimeMillis()
                val buffer = StringBuilder()

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim() ?: break
                    if (currentLine.startsWith("data: ")) {
                        val dataContent = currentLine.substring(6).trim()
                        if (dataContent == "[DONE]") {
                            if (buffer.isNotEmpty()) {
                                send(buffer.toString())
                                buffer.clear()
                            }
                            break
                        }
                        try {
                            val chunkText = parseChunkJson(dataContent)
                            if (chunkText != null) {
                                buffer.append(chunkText)
                                val now = System.currentTimeMillis()
                                // Batch UI updates to max 30 fps (every ~33ms) to prevent UI lag on fast APIs like Groq
                                if (now - lastEmit > 33) {
                                    send(buffer.toString())
                                    buffer.clear()
                                    lastEmit = now
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
                
                // Flush remaining tokens
                if (buffer.isNotEmpty()) {
                    send(buffer.toString())
                }
            }
        } catch (e: Exception) {
            send("Error: ${e.message ?: "An unexpected connection error occurred."}")
        }
    }.flowOn(Dispatchers.IO)

    private fun parseChunkJson(jsonStr: String): String? {
        if (jsonStr.isEmpty()) return null
        return try {
            val obj = JSONObject(jsonStr)
            val choices = obj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val delta = choice.optJSONObject("delta")
                if (delta != null && delta.has("content")) {
                    return delta.getString("content")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
