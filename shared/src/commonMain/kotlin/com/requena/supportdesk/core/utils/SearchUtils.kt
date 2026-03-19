package com.requena.supportdesk.core.utils

fun String.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return contains(query.trim(), ignoreCase = true)
}
