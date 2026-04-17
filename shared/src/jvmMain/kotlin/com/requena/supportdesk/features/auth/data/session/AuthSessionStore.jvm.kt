package com.requena.supportdesk.features.auth.data.session

import java.util.prefs.Preferences

actual class AuthSessionStore {
    private val preferences = Preferences.userRoot().node("com/requena/supportdesk/auth")

    actual fun read(): String? = preferences.get(KEY_AUTH_SESSION, null)

    actual fun write(value: String) {
        preferences.put(KEY_AUTH_SESSION, value)
        preferences.flush()
    }

    actual fun clear() {
        preferences.remove(KEY_AUTH_SESSION)
        preferences.flush()
    }

    private companion object {
        const val KEY_AUTH_SESSION = "auth_session"
    }
}
