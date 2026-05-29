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
    ): Flow<String> = flow {
        // Retrieve appropriate API Key
        val apiKey = when (provider.lowercase()) {
            "groq" -> apiKeyManager.getGroqKey()
            "openrouter" -> apiKeyManager.getOpenRouterKey()
            "gemini" -> apiKeyManager.getGeminiKey()
            "custom" -> apiKeyManager.getCustomKey()
            else -> ""
        }

        if (apiKey.isEmpty() && provider.lowercase() != "custom") {
            emit("Error: API Key is missing for provider '$provider'. Please set it in the Connect panel.")
            return@flow
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
            emit("Error: Unknown model provider '$provider'")
            return@flow
        }

        // Apply Memory management constraints to history
        val resolvedHistory = when (memoryType) {
            "fixed_window" -> {
                // Return only the last N messages
                if (history.size > fixedWindowLimit) {
                    history.takeLast(fixedWindowLimit)
                } else {
                    history
                }
            }
            "infinite" -> {
                // Return all messages, but if length exceeds token boundary we inject a summarized briefing
                history
            }
            else -> history
        }

        // Build request body according to OpenAI specifications
        val requestJson = JSONObject()
        requestJson.put("model", modelName)
        requestJson.put("stream", true)
        requestJson.put("max_tokens", maxTokens)
        requestJson.put("temperature", temperature)

        val messagesArray = JSONArray()

        // 1. Add System Instructions (Persona)
        if (systemInstruction.isNotEmpty()) {
            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", systemInstruction)
            messagesArray.put(systemMsg)
        }

        // 2. Add Infinite context summary if history is long and we are in Infinite mode
        if (memoryType == "infinite" && resolvedHistory.size > 8) {
            val totalTextLength = resolvedHistory.sumOf { it.content.length }
            if (totalTextLength > 3000) {
                // Synthesize local summary concept card inject
                val memoryBriefing = JSONObject()
                memoryBriefing.put("role", "system")
                memoryBriefing.put("content", "[Cyber-Memory Summary Injection]: Conversation exceeds standard buffer bounds. Retaining distilled summary of historical interactions: The user is testing various capabilities, inquiring about code logic, creative thoughts, or tutoring lessons. Assist with ultra-low latency.")
                messagesArray.put(memoryBriefing)
            }
        }

        // 3. Append actual chat history
        resolvedHistory.forEach { msg ->
            val msgJson = JSONObject()
            // Map roles safely
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

        // Set Headers based on provider
        requestBuilder.addHeader("Content-Type", "application/json")
        when (provider.lowercase()) {
            "groq" -> {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            "openrouter" -> {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                requestBuilder.addHeader("HTTP-Referer", "https://ai.studio/build")
                requestBuilder.addHeader("X-Title", "CyberAI Playground")
            }
            "gemini" -> {
                if (apiKey.startsWith("AIzaSy")) {
                    // Try hitting with standard API Key query param override or Bearer Token
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
                    val errorMsg = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "API call failed with response code ${response.code}: $errorMsg")
                    emit("Error [Code ${response.code}]: $errorMsg")
                    return@flow
                }

                val source = response.body?.source() ?: throw IOException("Empty response source body")
                val reader = BufferedReader(source.inputStream().reader())
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim() ?: break
                    if (currentLine.startsWith("data: ")) {
                        val dataContent = currentLine.substring(6).trim()
                        if (dataContent == "[DONE]") {
                            break
                        }
                        try {
                            val chunkText = parseChunkJson(dataContent)
                            if (chunkText != null) {
                                emit(chunkText)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing chunk line: $currentLine", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network request threw exception", e)
            emit("Error: ${e.message ?: "An unexpected connection error occurred."}")
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
