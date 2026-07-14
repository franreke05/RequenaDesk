package com.requena.supportdesk.server.config

import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.Properties

private object ServerDistributionAnchor

fun loadServerProperties(): Map<String, String> {
    val loaded = Properties()
    serverPropertiesCandidates()
        .firstOrNull(Files::exists)
        ?.let { path ->
            Files.newBufferedReader(path, StandardCharsets.UTF_8).use(loaded::load)
        }
    return loaded.stringPropertyNames().associate { rawKey ->
        rawKey.removePrefix("\uFEFF") to loaded.getProperty(rawKey)
    }
}

private fun serverPropertiesCandidates(): List<Path> {
    val userDir = runCatching { Path.of(System.getProperty("user.dir")) }.getOrNull()
    val codeSourceDir = runCatching {
        Path.of(
            ServerDistributionAnchor::class.java.protectionDomain.codeSource.location.toURI(),
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
