package com.requena.supportdesk.features.invoices.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class InvoiceHoursTest {
    @Test
    fun roundsPartialRecordedHoursUpAndDoesNotExposeMinutesOrSeconds() {
        assertEquals(0, roundedInvoiceHours(0))
        assertEquals(1, roundedInvoiceHours(1))
        assertEquals(1, roundedInvoiceHours(3_600))
        assertEquals(2, roundedInvoiceHours(3_601))
        assertEquals(3, roundedInvoiceHours(10_799))
    }

    @Test
    fun treatsInvalidNegativeDurationAsZero() {
        assertEquals(0, roundedInvoiceHours(-1))
    }
}
