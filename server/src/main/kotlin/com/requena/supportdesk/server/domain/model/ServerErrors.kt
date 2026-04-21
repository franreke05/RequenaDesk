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
