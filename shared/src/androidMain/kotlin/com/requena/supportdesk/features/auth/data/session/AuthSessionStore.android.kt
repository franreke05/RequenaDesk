package com.requena.supportdesk.features.auth.data.session

import android.content.Context

actual class AuthSessionStore {
    actual fun read(): String? = preferences()?.getString(KEY_AUTH_SESSION, null)

    actual fun write(value: String) {
        preferences()
            ?.edit()
            ?.putString(KEY_AUTH_SESSION, value)
            ?.apply()
    }

    actual fun clear() {
        preferences()
            ?.edit()
            ?.remove(KEY_AUTH_SESSION)
            ?.apply()
    }

    private fun preferences() = applicationContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCES_NAME = "orykai_mobile_auth"
        private const val KEY_AUTH_SESSION = "auth_session"

        private var applicationContext: Context? = null

        fun initialize(context: Context) {
            applicationContext = context.applicationContext
        }
    }
}
