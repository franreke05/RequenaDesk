package com.requena.supportdesk.features.auth.data.session

expect class AuthSessionStore() {
    fun read(): String?
    fun write(value: String)
    fun clear()
}
