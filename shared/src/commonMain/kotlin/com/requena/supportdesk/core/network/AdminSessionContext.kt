package com.requena.supportdesk.core.network

import com.requena.supportdesk.core.model.User

object AdminSessionContext {
    private var currentUser: User? = null

    fun update(user: User?) {
        currentUser = user
    }

    fun currentUserId(): String? = currentUser?.id

    fun currentUser(): User? = currentUser
}
