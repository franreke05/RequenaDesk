package com.requena.supportdesk.core.network

import com.requena.supportdesk.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdminSessionContext {
    private val _user = MutableStateFlow<User?>(null)

    /** Signed-in user; also updates when the session is refreshed (e.g. a new company role). */
    val user: StateFlow<User?> = _user.asStateFlow()

    fun update(user: User?) {
        _user.value = user
    }

    fun currentUserId(): String? = _user.value?.id

    fun currentUser(): User? = _user.value
}
