package com.requena.supportdesk.features.auth.data.session

actual class AuthSessionStore {
    actual fun read(): String? = null

    actual fun write(value: String) = Unit

    actual fun clear() = Unit
}
