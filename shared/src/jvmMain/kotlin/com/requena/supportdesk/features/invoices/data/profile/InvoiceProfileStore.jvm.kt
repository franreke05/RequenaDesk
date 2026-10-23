package com.requena.supportdesk.features.invoices.data.profile

import com.requena.supportdesk.features.invoices.domain.model.InvoiceIssuer
import com.requena.supportdesk.features.invoices.domain.model.InvoiceRecipient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Properties

actual fun createInvoiceProfileStore(): InvoiceProfileStore = DesktopInvoiceProfileStore()

/**
 * Stores the data in `~/.supportdesk/facturacion-datos.properties` (UTF-8). The legacy
 * `invoice.issuer.*` keys of `supportdesk.properties` are still read as a fallback so an
 * existing local setup keeps working.
 */
internal class DesktopInvoiceProfileStore(
    private val file: Path = defaultProfileFile(),
    private val legacyFiles: List<Path> = legacyPropertiesCandidates(),
) : InvoiceProfileStore {
    private val mutex = Mutex()

    override suspend fun loadIssuer(): InvoiceIssuer = mutex.withLock {
        val properties = read(file)
        if (properties.getProperty(ISSUER_PREFIX + "fullName") == null) return@withLock legacyIssuer()
        InvoiceIssuer(
            fullName = properties.value(ISSUER_PREFIX + "fullName"),
            tradeName = properties.value(ISSUER_PREFIX + "tradeName"),
            taxId = properties.value(ISSUER_PREFIX + "taxId"),
            addressLine = properties.value(ISSUER_PREFIX + "addressLine"),
            postalCode = properties.value(ISSUER_PREFIX + "postalCode"),
            city = properties.value(ISSUER_PREFIX + "city"),
            province = properties.value(ISSUER_PREFIX + "province"),
            country = properties.value(ISSUER_PREFIX + "country", InvoiceIssuer.DEFAULT_COUNTRY),
            email = properties.value(ISSUER_PREFIX + "email"),
            phone = properties.value(ISSUER_PREFIX + "phone"),
            paymentMethod = properties.value(ISSUER_PREFIX + "paymentMethod", InvoiceIssuer.DEFAULT_PAYMENT_METHOD),
            iban = properties.value(ISSUER_PREFIX + "iban"),
        )
    }

    override suspend fun saveIssuer(issuer: InvoiceIssuer) = mutex.withLock {
        val properties = read(file)
        mapOf(
            "fullName" to issuer.fullName,
            "tradeName" to issuer.tradeName,
            "taxId" to issuer.taxId,
            "addressLine" to issuer.addressLine,
            "postalCode" to issuer.postalCode,
            "city" to issuer.city,
            "province" to issuer.province,
            "country" to issuer.country,
            "email" to issuer.email,
            "phone" to issuer.phone,
            "paymentMethod" to issuer.paymentMethod,
            "iban" to issuer.iban,
        ).forEach { (key, value) -> properties.setProperty(ISSUER_PREFIX + key, value.trim()) }
        write(properties)
    }

    override suspend fun loadRecipient(clientId: String): InvoiceRecipient? = mutex.withLock {
        val properties = read(file)
        val prefix = recipientPrefix(clientId)
        if (properties.getProperty(prefix + "legalName") == null) return@withLock null
        InvoiceRecipient(
            legalName = properties.value(prefix + "legalName"),
            taxId = properties.value(prefix + "taxId"),
            addressLine = properties.value(prefix + "addressLine"),
            postalCode = properties.value(prefix + "postalCode"),
            city = properties.value(prefix + "city"),
            province = properties.value(prefix + "province"),
            country = properties.value(prefix + "country", InvoiceIssuer.DEFAULT_COUNTRY),
            email = properties.value(prefix + "email"),
        )
    }

    override suspend fun saveRecipient(clientId: String, recipient: InvoiceRecipient) = mutex.withLock {
        val properties = read(file)
        val prefix = recipientPrefix(clientId)
        mapOf(
            "legalName" to recipient.legalName,
            "taxId" to recipient.taxId,
            "addressLine" to recipient.addressLine,
            "postalCode" to recipient.postalCode,
            "city" to recipient.city,
            "province" to recipient.province,
            "country" to recipient.country,
            "email" to recipient.email,
        ).forEach { (key, value) -> properties.setProperty(prefix + key, value.trim()) }
        write(properties)
    }

    private fun legacyIssuer(): InvoiceIssuer {
        val legacy = legacyFiles.firstOrNull(Files::exists)?.let(::read) ?: return InvoiceIssuer()
        return InvoiceIssuer(
            fullName = legacy.value("invoice.issuer.name"),
            taxId = legacy.value("invoice.issuer.taxId"),
            addressLine = legacy.value("invoice.issuer.addressLine1"),
            city = legacy.value("invoice.issuer.addressLine2"),
            phone = legacy.value("invoice.issuer.phone"),
        )
    }

    private fun read(path: Path): Properties = Properties().also { properties ->
        if (Files.exists(path)) Files.newBufferedReader(path, Charsets.UTF_8).use(properties::load)
    }

    private fun write(properties: Properties) {
        Files.createDirectories(file.toAbsolutePath().parent)
        val temporary = Files.createTempFile(file.toAbsolutePath().parent, ".facturacion-", ".tmp")
        try {
            Files.newBufferedWriter(temporary, Charsets.UTF_8).use { writer ->
                properties.store(writer, "Datos fiscales locales para las facturas de OryKai")
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun recipientPrefix(clientId: String): String =
        "recipient.${clientId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }}."

    private fun Properties.value(key: String, default: String = ""): String =
        getProperty(key)?.trim()?.takeIf(String::isNotEmpty) ?: default

    private companion object {
        const val ISSUER_PREFIX = "issuer."
    }
}

private fun userHome(): Path = System.getProperty("user.home")
    ?.takeIf(String::isNotBlank)
    ?.let(Paths::get)
    ?: Paths.get(".")

private fun defaultProfileFile(): Path = userHome().resolve(".supportdesk").resolve("facturacion-datos.properties")

private fun legacyPropertiesCandidates(): List<Path> = listOfNotNull(
    System.getProperty("user.dir")?.takeIf(String::isNotBlank)?.let { Paths.get(it).resolve("supportdesk.properties") },
    userHome().resolve(".supportdesk").resolve("supportdesk.properties"),
).distinct()
