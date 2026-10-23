package com.requena.supportdesk.features.invoices.domain.model

import com.requena.supportdesk.core.model.Client

/**
 * Recipient prefilled from the fiscal data stored with the client. Used when this machine has
 * no locally remembered recipient for the client yet.
 */
fun Client.defaultInvoiceRecipient(): InvoiceRecipient = InvoiceRecipient(
    legalName = billing.legalName.ifBlank { companyName },
    taxId = billing.taxId,
    addressLine = billing.addressLine,
    postalCode = billing.postalCode,
    city = billing.city,
    province = billing.province,
    country = billing.country.ifBlank { InvoiceIssuer.DEFAULT_COUNTRY },
    email = billing.email,
)
