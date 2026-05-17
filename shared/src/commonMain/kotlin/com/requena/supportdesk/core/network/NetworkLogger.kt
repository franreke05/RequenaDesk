package com.requena.supportdesk.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun addLog(message: String) {
        val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        val logEntry = "[$timestamp] $message"
        println(logEntry)
        _logs.value = (_logs.value + logEntry).takeLast(50)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
