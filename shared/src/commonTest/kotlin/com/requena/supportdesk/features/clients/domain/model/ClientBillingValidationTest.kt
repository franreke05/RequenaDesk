package com.requena.supportdesk.features.clients.domain.model

import com.requena.supportdesk.core.model.ClientBillingProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// Invented test data only.
class ClientBillingValidationTest {
    @Test
    fun normalizesTaxIdAndTrimsFields() {
        val normalized = ClientBillingProfile(
            legalName = "  Ejemplo Pruebas S.L. ",
            taxId = " b-0000000.0 ",
            city = " Villa Prueba ",
            email = " facturas@example.com ",
        ).normalized()

        assertEquals("Ejemplo Pruebas S.L.", normalized.legalName)
        assertEquals("B00000000", normalized.taxId)
        assertEquals("Villa Prueba", normalized.city)
        assertEquals("facturas@example.com", normalized.email)
    }

    @Test
    fun emptyFiscalDataIsValid() {
        assertNull(ClientBillingProfile().validationProblem())
    }

    @Test
    fun acceptsValidDniAndCompanyTaxIds() {
        assertNull(clientTaxIdProblem("12345678-z"))
        assertNull(clientTaxIdProblem("X0000000T"))
        assertNull(clientTaxIdProblem("B00000000"))
    }

    @Test
    fun rejectsDniWithWrongControlLetter() {
        assertNotNull(clientTaxIdProblem("12345678A"))
        assertNotNull(ClientBillingProfile(taxId = "12345678A").validationProblem())
    }

    @Test
    fun rejectsTaxIdsWithImpossibleLength() {
        assertNotNull(clientTaxIdProblem("B1"))
        assertNotNull(clientTaxIdProblem("A".repeat(21)))
    }

    @Test
    fun rejectsMalformedBillingEmail() {
        assertNotNull(clientBillingEmailProblem("facturas@"))
        assertNull(clientBillingEmailProblem(""))
        assertNotNull(ClientBillingProfile(email = "no-es-email").validationProblem())
    }
}
