package com.requena.supportdesk.core.time

import java.time.LocalDate

actual fun currentIsoDate(): String = LocalDate.now().toString()
