package com.requena.supportdesk.features.business.finance.presentation

import kotlin.random.Random

sealed interface BusinessFinanceUiEffect {
    data class ShowMessage(val message: String) : BusinessFinanceUiEffect
    data object AccessDenied : BusinessFinanceUiEffect
}

fun interface BusinessFinanceIdempotencyKeyFactory {
    fun create(operation: String): String
}

object RandomBusinessFinanceIdempotencyKeyFactory : BusinessFinanceIdempotencyKeyFactory {
    override fun create(operation: String): String = buildString {
        append("business-finance-")
        append(operation)
        repeat(4) {
            append('-')
            append(Random.nextInt().toUInt().toString(16))
        }
    }
}
