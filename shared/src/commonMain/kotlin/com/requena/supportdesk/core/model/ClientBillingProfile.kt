package com.requena.supportdesk.core.model

/**
 * Fiscal data of a client, needed to invoice companies: legal name, NIF/CIF and full address.
 * Every field is optional because not every client is invoiced.
 */
data class ClientBillingProfile(
    val legalName: String = "",
    val taxId: String = "",
    val addressLine: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val email: String = "",
) {
    val isEmpty: Boolean
        get() = listOf(legalName, taxId, addressLine, postalCode, city, province, country, email)
            .all(String::isBlank)
}
