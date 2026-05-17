package com.requena.supportdesk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate

actual fun createSupportDeskHttpClient(): HttpClient {
    val trustAllCertificates = arrayOf<X509TrustManager>(object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
    })

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, trustAllCertificates, SecureRandom())
    }

    return HttpClient(CIO) {
        engine {
            https {
                trustManager = trustAllCertificates[0]
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
    }
}

actual fun supportDeskBaseUrl(): String {
    val envUrl = System.getenv("SUPPORTDESK_BASE_URL")
        ?.trim()
        ?.takeIf(String::isNotBlank)
    return (envUrl ?: "https://crm.franciscorequena.cloud").removeSuffix("/")
}
