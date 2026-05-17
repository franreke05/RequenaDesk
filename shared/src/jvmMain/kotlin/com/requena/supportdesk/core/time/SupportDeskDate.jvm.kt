package com.requena.supportdesk.core.time

import java.time.Instant
import java.time.LocalDate

actual fun currentIsoDate(): String = LocalDate.now().toString()

actual fun currentIsoDateTime(): String = Instant.now().toString()
