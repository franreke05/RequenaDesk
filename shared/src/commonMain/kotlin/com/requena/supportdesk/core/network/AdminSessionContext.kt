package com.requena.supportdesk.core.network

import com.requena.supportdesk.core.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AdminSessionContext {
    private var currentUser: User? = null

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredFlow: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun update(user: User?) {
        currentUser = user
    }

    fun currentUserId(): String? = currentUser?.id

    fun currentUser(): User? = currentUser

    fun notifySessionExpired() {
        update(null)
        _sessionExpired.tryEmit(Unit)
    }
}
