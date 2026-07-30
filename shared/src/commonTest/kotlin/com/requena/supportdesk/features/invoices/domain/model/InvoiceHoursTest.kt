package com.requena.supportdesk.features.invoices.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvoiceHoursTest {
    @Test
    fun roundsUpToTheNextQuarterHourPerTheSpecTable() {
        assertEquals(0, roundUpToQuarterHour(0))
        assertEquals(15, roundUpToQuarterHour(1))
        assertEquals(15, roundUpToQuarterHour(14))
        assertEquals(15, roundUpToQuarterHour(15))
        assertEquals(30, roundUpToQuarterHour(16))
        assertEquals(30, roundUpToQuarterHour(29))
        assertEquals(30, roundUpToQuarterHour(30))
        assertEquals(45, roundUpToQuarterHour(31))
        assertEquals(45, roundUpToQuarterHour(44))
        assertEquals(45, roundUpToQuarterHour(45))
        assertEquals(60, roundUpToQuarterHour(46))
        assertEquals(60, roundUpToQuarterHour(59))
        assertEquals(60, roundUpToQuarterHour(60))
        assertEquals(75, roundUpToQuarterHour(61))
        assertEquals(75, roundUpToQuarterHour(67))
        assertEquals(75, roundUpToQuarterHour(75))
    }

    @Test
    fun treatsInvalidNegativeDurationAsZero() {
        assertEquals(0, roundUpToQuarterHour(-1))
    }

    @Test
    fun sumsBeforeRoundingSoTwoSmallSessionsRoundOnce() {
        // Sesion A: 7 min. Sesion B: 8 min. Total real: 15 min -> should round to 15, not 30.
        val sessionA = 7
        val sessionB = 8
        assertEquals(15, roundUpToQuarterHour(sessionA + sessionB))
        // The wrong (forbidden) approach of rounding each session individually would give 30.
        assertTrue(roundUpToQuarterHour(sessionA) + roundUpToQuarterHour(sessionB) == 30)
    }

    @Test
    fun oneHourSevenMinutesRoundsToOneHourFifteen() {
        assertEquals(75, roundUpToQuarterHour(67))
        assertEquals(1.25, quarterHourMinutesToDecimalHours(roundUpToQuarterHour(67)))
    }

    @Test
    fun decimalHoursConversionIsExactForQuarterHourSteps() {
        assertEquals(0.0, quarterHourMinutesToDecimalHours(0))
        assertEquals(0.25, quarterHourMinutesToDecimalHours(15))
        assertEquals(0.5, quarterHourMinutesToDecimalHours(30))
        assertEquals(0.75, quarterHourMinutesToDecimalHours(45))
        assertEquals(1.0, quarterHourMinutesToDecimalHours(60))
        assertEquals(4.0, quarterHourMinutesToDecimalHours(240))
    }
}
