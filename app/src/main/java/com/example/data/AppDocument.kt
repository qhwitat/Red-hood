package com.example.data

import java.util.UUID

data class AppDocument(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val text: String,
    val sizeBytes: Long,
    val charCount: Int,
    val importedAt: Long = System.currentTimeMillis(),
    val attachedSessions: Set<String> = emptySet()
)
