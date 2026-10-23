package com.requena.supportdesk.features.clients.domain.model

import com.requena.supportdesk.core.model.ClientBillingProfile
import com.requena.supportdesk.features.invoices.domain.model.isSpanishPersonalTaxIdValid
import com.requena.supportdesk.features.invoices.domain.model.normalizeTaxId

/** Trims every field and stores the NIF/CIF normalized (upper case, letters and digits only). */
fun ClientBillingProfile.normalized(): ClientBillingProfile = ClientBillingProfile(
    legalName = legalName.trim(),
    taxId = normalizeTaxId(taxId),
    addressLine = addressLine.trim(),
    postalCode = postalCode.trim(),
    city = city.trim(),
    province = province.trim(),
    country = country.trim(),
    email = email.trim(),
)

/** Problem with the NIF/CIF, or null when it is empty or looks valid. */
fun clientTaxIdProblem(taxId: String): String? {
    val normalized = normalizeTaxId(taxId)
    return when {
        normalized.isEmpty() -> null
        normalized.length !in TAX_ID_LENGTH -> "El NIF/CIF debe tener entre 5 y 20 letras o números."
        isSpanishPersonalTaxIdValid(normalized) == false -> "El NIF/NIE no es válido: revisa los números y la letra."
        else -> null
    }
}

/** Problem with the billing email, or null when it is empty or looks valid. */
fun clientBillingEmailProblem(email: String): String? {
    val trimmed = email.trim()
    return if (trimmed.isEmpty() || BILLING_EMAIL_PATTERN.matches(trimmed)) null else "El email de facturación no es válido."
}

/** First problem of the fiscal data, or null when it can be saved. */
fun ClientBillingProfile.validationProblem(): String? =
    clientTaxIdProblem(taxId) ?: clientBillingEmailProblem(email)

private val TAX_ID_LENGTH = 5..20
private val BILLING_EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
