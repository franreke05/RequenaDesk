package com.requena.supportdesk.server.config

const val DEFAULT_SERVER_PORT = 8080
const val DEFAULT_SERVER_HOST = "127.0.0.1"
const val API_VERSION = "v0"

fun resolveServerPort(
    environment: Map<String, String> = System.getenv(),
    properties: Map<String, String> = loadServerProperties() + System.getProperties().stringPropertyNames().associateWith(System::getProperty),
): Int = (
    environment["SUPPORTDESK_SERVER_PORT"]
        ?: properties["SUPPORTDESK_SERVER_PORT"]
        ?: environment["PORT"]
        ?: properties["PORT"]
    )?.toIntOrNull()
    ?: DEFAULT_SERVER_PORT

fun resolveServerHost(
    environment: Map<String, String> = System.getenv(),
    properties: Map<String, String> = loadServerProperties() + System.getProperties().stringPropertyNames().associateWith(System::getProperty),
): String = (
    environment["SUPPORTDESK_SERVER_HOST"]
        ?: properties["SUPPORTDESK_SERVER_HOST"]
        ?: environment["HOST"]
        ?: properties["HOST"]
    )?.trim()
    ?.takeIf(String::isNotBlank)
    ?: DEFAULT_SERVER_HOST
