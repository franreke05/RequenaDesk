package com.requena.supportdesk.core.push

actual object PushTokenStore {
    actual fun currentToken(): String? = null
    actual fun update(token: String) {}
}
