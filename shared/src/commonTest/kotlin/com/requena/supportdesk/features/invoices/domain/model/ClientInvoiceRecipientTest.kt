package com.requena.supportdesk.features.invoices.domain.model

import com.requena.supportdesk.core.model.Client
import com.requena.supportdesk.core.model.ClientBillingProfile
import com.requena.supportdesk.features.clients.data.dto.ClientBillingDto
import com.requena.supportdesk.features.clients.data.dto.ClientDto
import com.requena.supportdesk.features.clients.data.mapper.ClientsMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Invented test data only.
class ClientInvoiceRecipientTest {
    private val billing = ClientBillingProfile(
        legalName = "Ejemplo Pruebas S.L.",
        taxId = "B00000000",
        addressLine = "Calle Inventada 1",
        postalCode = "00000",
        city = "Villa Prueba",
        province = "Provincia Prueba",
        country = "Portugal",
        email = "facturas@example.com",
    )

    @Test
    fun recipientIsPrefilledFromClientFiscalData() {
        val recipient = client(billing).defaultInvoiceRecipient()

        assertEquals(
            InvoiceRecipient(
                legalName = "Ejemplo Pruebas S.L.",
                taxId = "B00000000",
                addressLine = "Calle Inventada 1",
                postalCode = "00000",
                city = "Villa Prueba",
                province = "Provincia Prueba",
                country = "Portugal",
                email = "facturas@example.com",
            ),
            recipient,
        )
        assertTrue(recipient.missingRequiredFields().isEmpty())
    }

    @Test
    fun clientWithoutFiscalDataFallsBackToCompanyNameAndDefaultCountry() {
        val recipient = client(ClientBillingProfile()).defaultInvoiceRecipient()

        assertEquals("Empresa Ficticia", recipient.legalName)
        assertEquals(InvoiceIssuer.DEFAULT_COUNTRY, recipient.country)
        assertEquals("", recipient.taxId)
    }

    @Test
    fun fiscalDataSurvivesTheApiMapping() {
        val dto = ClientDto(
            id = "client-test",
            ownerAdminId = "admin-test",
            companyName = "Empresa Ficticia",
            email = "contacto@example.com",
            billing = ClientsMapper.toDto(billing),
        )

        assertEquals(billing, ClientsMapper.fromDto(dto).billing)
        assertEquals(ClientBillingDto(), ClientDto("id", "owner", "Empresa", email = "a@example.com").billing)
    }

    private fun client(billing: ClientBillingProfile) = Client(
        id = "client-test",
        companyName = "Empresa Ficticia",
        contactName = "Contacto Ficticio",
        email = "contacto@example.com",
        billing = billing,
    )
}
