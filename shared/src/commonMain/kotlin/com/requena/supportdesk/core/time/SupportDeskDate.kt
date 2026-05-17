package com.requena.supportdesk.core.time

expect fun currentIsoDate(): String

expect fun currentIsoDateTime(): String

fun isPastIsoDate(isoDate: String, todayIsoDate: String = currentIsoDate()): Boolean = isoDate < todayIsoDate

fun isFutureIsoDate(isoDate: String, todayIsoDate: String = currentIsoDate()): Boolean = isoDate > todayIsoDate

fun isTodayIsoDate(isoDate: String, todayIsoDate: String = currentIsoDate()): Boolean = isoDate == todayIsoDate
