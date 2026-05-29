package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow

object AppConfig {
    val isArabic = MutableStateFlow(false)
    val errorLogs = MutableStateFlow<List<String>>(emptyList())

    fun logError(msg: String) {
        val current = errorLogs.value.toMutableList()
        current.add(0, msg)
        if (current.size > 50) current.removeLast()
        errorLogs.value = current
    }
}
