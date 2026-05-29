package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val personaId: String,
    val provider: String, // "gemini", "groq", "openrouter"
    val modelName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val memoryType: String = "fixed_window", // "fixed_window", "infinite"
    val maxTokensToRemember: Int = 4000,
    val fixedWindowLimit: Int = 10 // Last N messages
)
