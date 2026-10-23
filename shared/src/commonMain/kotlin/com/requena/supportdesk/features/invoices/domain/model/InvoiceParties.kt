package com.requena.supportdesk.features.invoices.domain.model

/**
 * Datos fiscales de quien emite la factura (el autónomo). Exigidos por el art. 6 del
 * RD 1619/2012: nombre y apellidos, NIF y domicilio. El resto mejora la factura pero es opcional.
 */
data class InvoiceIssuer(
    val fullName: String = "",
    val tradeName: String = "",
    val taxId: String = "",
    val addressLine: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = DEFAULT_COUNTRY,
    val email: String = "",
    val phone: String = "",
    val paymentMethod: String = DEFAULT_PAYMENT_METHOD,
    val iban: String = "",
) {
    /** Etiquetas de los campos obligatorios que faltan, en el orden del formulario. */
    fun missingRequiredFields(): List<String> = buildList {
        if (fullName.isBlank()) add("nombre y apellidos")
        if (taxId.isBlank()) add("NIF")
        if (addressLine.isBlank()) add("domicilio")
        if (postalCode.isBlank()) add("código postal")
        if (city.isBlank()) add("población")
    }

    companion object {
        const val DEFAULT_COUNTRY = "España"
        const val DEFAULT_PAYMENT_METHOD = "Transferencia bancaria"
    }
}

/**
 * Datos fiscales del destinatario. Para clientes empresa o autónomo la factura completa debe
 * incluir razón social, NIF y domicilio.
 */
data class InvoiceRecipient(
    val legalName: String = "",
    val taxId: String = "",
    val addressLine: String = "",
    val postalCode: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = InvoiceIssuer.DEFAULT_COUNTRY,
    /** Email de facturación; no es obligatorio en la factura pero se usa para enviarla. */
    val email: String = "",
) {
    fun missingRequiredFields(): List<String> = buildList {
        if (legalName.isBlank()) add("razón social del cliente")
        if (taxId.isBlank()) add("NIF/CIF del cliente")
        if (addressLine.isBlank()) add("domicilio del cliente")
        if (postalCode.isBlank() && city.isBlank()) add("población del cliente")
    }
}

/**
 * Legal checks that must pass before an invoice number is used, so a wrong form never burns a
 * number of the correlative series. Returns a message for the user, or null when valid.
 */
fun invoiceLegalProblem(input: CreateInvoiceInput): String? {
    input.issuer.missingRequiredFields().takeIf(List<String>::isNotEmpty)?.let { missing ->
        return "Faltan tus datos fiscales: ${missing.joinToString()}."
    }
    if (isSpanishPersonalTaxIdValid(input.issuer.taxId) == false) {
        return "Tu NIF no es válido: revisa los números y la letra."
    }
    input.recipient.missingRequiredFields().takeIf(List<String>::isNotEmpty)?.let { missing ->
        return "Faltan datos fiscales del cliente: ${missing.joinToString()}."
    }
    if (!input.withholdingPercent.isFinite() || input.withholdingPercent !in 0.0..100.0) {
        return "La retención de IRPF debe estar entre 0 y 100."
    }
    if (input.taxPercent == 0.0 && input.taxExemptionNote.isNullOrBlank()) {
        return "Con IVA al 0 % indica el motivo legal (exención o inversión del sujeto pasivo)."
    }
    return null
}

/** Normaliza un NIF/CIF/NIE: mayúsculas y sin espacios, guiones ni puntos. */
fun normalizeTaxId(value: String): String = value.uppercase().filter { it.isLetterOrDigit() }

/**
 * Comprueba la letra de control de un DNI (8 dígitos + letra) o NIE (X/Y/Z + 7 dígitos + letra).
 * Devuelve null si el formato no es de persona física española (p. ej. CIF o VAT extranjero),
 * para que solo se avise cuando sabemos validar.
 */
fun isSpanishPersonalTaxIdValid(value: String): Boolean? {
    val taxId = normalizeTaxId(value)
    val digits = when {
        DNI_PATTERN.matches(taxId) -> taxId.dropLast(1)
        NIE_PATTERN.matches(taxId) -> NIE_PREFIXES.indexOf(taxId.first()).toString() + taxId.substring(1, 8)
        else -> return null
    }
    val expected = DNI_CONTROL_LETTERS[digits.toInt() % DNI_CONTROL_LETTERS.length]
    return taxId.last() == expected
}

private val DNI_PATTERN = Regex("^[0-9]{8}[A-Z]$")
private val NIE_PATTERN = Regex("^[XYZ][0-9]{7}[A-Z]$")
private const val NIE_PREFIXES = "XYZ"
private const val DNI_CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE"
