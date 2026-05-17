package com.requena.supportdesk.core.push

import android.content.Context

actual object PushTokenStore {
    private const val PREFERENCES_NAME = "orykai_mobile_push"
    private const val KEY_PUSH_TOKEN = "push_token"

    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    actual fun currentToken(): String? = preferences()?.getString(KEY_PUSH_TOKEN, null)

    actual fun update(token: String) {
        preferences()?.edit()?.putString(KEY_PUSH_TOKEN, token)?.apply()
    }

    private fun preferences() = applicationContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
