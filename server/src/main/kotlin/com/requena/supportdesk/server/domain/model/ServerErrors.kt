package com.requena.supportdesk.server.domain.model

class ServerNotFoundException(
    override val message: String,
) : RuntimeException(message)

class ServerConflictException(
    override val message: String,
) : RuntimeException(message)

class ServerValidationException(
    override val message: String,
) : RuntimeException(message)

/** The caller is authenticated but its company role does not allow the action (HTTP 403). */
class ServerForbiddenException(
    override val message: String,
) : RuntimeException(message)
