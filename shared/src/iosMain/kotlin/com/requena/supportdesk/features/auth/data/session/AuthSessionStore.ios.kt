package com.requena.supportdesk.features.auth.data.session

import platform.Foundation.NSUserDefaults

actual class AuthSessionStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun read(): String? = defaults.stringForKey(KEY_AUTH_SESSION)

    actual fun write(value: String) {
        defaults.setObject(value, forKey = KEY_AUTH_SESSION)
    }

    actual fun clear() {
        defaults.removeObjectForKey(KEY_AUTH_SESSION)
    }

    private companion object {
        const val KEY_AUTH_SESSION = "orykai_auth_session"
    }
}
