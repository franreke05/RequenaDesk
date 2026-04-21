package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

actual fun createSupportDeskHttpClient(): HttpClient = HttpClient(CIO)

actual fun supportDeskBaseUrl(): String = resolveJvmSupportDeskBaseUrl()

private object DesktopDistributionAnchor

private fun resolveJvmSupportDeskBaseUrl(): String {
    val configuredUrl = configuredBaseUrl()
        ?: "http://127.0.0.1:8080"
    return configuredUrl.removeSuffix("/")
}

private fun configuredBaseUrl(): String? {
    val propertyValue = System.getProperty("supportdesk.baseUrl")
        ?.trim()
        ?.takeIf(String::isNotBlank)
    if (propertyValue != null) return propertyValue

    val envValue = System.getenv("SUPPORTDESK_BASE_URL")
        ?.trim()
        ?.takeIf(String::isNotBlank)
    if (envValue != null) return envValue

    val properties = loadDesktopProperties()
    return listOf("supportdesk.baseUrl", "baseUrl")
        .firstNotNullOfOrNull { key ->
            properties.getProperty(key)
                ?.trim()
                ?.takeIf(String::isNotBlank)
        }
}

private fun loadDesktopProperties(): Properties {
    val properties = Properties()
    desktopPropertiesCandidates()
        .firstOrNull(Files::exists)
        ?.let { path ->
            Files.newInputStream(path).use(properties::load)
        }
    return properties
}

private fun desktopPropertiesCandidates(): List<Path> {
    val userDir = runCatching { Path.of(System.getProperty("user.dir")) }.getOrNull()
    val codeSourceDir = runCatching {
        Path.of(
            DesktopDistributionAnchor::class.java.protectionDomain.codeSource.location.toURI(),
        ).parent
    }.getOrNull()
    val applicationRoot = codeSourceDir?.parent
    val userHome = System.getProperty("user.home")
        ?.takeIf(String::isNotBlank)
        ?.let { Path.of(it, ".supportdesk") }

    return buildList {
        if (userDir != null) add(userDir.resolve("supportdesk.properties"))
        if (codeSourceDir != null) add(codeSourceDir.resolve("supportdesk.properties"))
        if (applicationRoot != null) add(applicationRoot.resolve("supportdesk.properties"))
        if (userHome != null) add(userHome.resolve("supportdesk.properties"))
    }.distinct()
}
