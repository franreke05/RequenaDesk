package com.requena.supportdesk.core.model

data class TaskLabel(
    val id: String,
    val name: String,
    val colorHex: String,
    val tasksCount: Int = 0,
)

typealias TaskCategory = TaskLabel
