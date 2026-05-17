package com.requena.supportdesk.core.push

expect object PushTokenStore {
    fun currentToken(): String?
    fun update(token: String)
}
