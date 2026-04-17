package com.requena.supportdesk.server.config

import java.net.URI

data class DatabaseSettings(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)

data class ServerEnvironment(
    val database: DatabaseSettings?,
    val bootstrapDemoData: Boolean,
    val bootstrapAdminPassword: String,
    val bootstrapClientPassword: String,
) {
    companion object {
        fun load(
            environment: Map<String, String> = System.getenv(),
            properties: Map<String, String> = System.getProperties().stringPropertyNames().associateWith(System::getProperty),
        ): ServerEnvironment = ServerEnvironment(
            database = resolveDatabaseSettings(environment, properties),
            bootstrapDemoData = value("SUPPORTDESK_BOOTSTRAP_DEMO_DATA", environment, properties)?.toBooleanStrictOrNull() == true,
            bootstrapAdminPassword = value("SUPPORTDESK_BOOTSTRAP_ADMIN_PASSWORD", environment, properties) ?: "Admin1234!",
            bootstrapClientPassword = value("SUPPORTDESK_BOOTSTRAP_CLIENT_PASSWORD", environment, properties) ?: "Client1234!",
        )

        private fun resolveDatabaseSettings(
            environment: Map<String, String>,
            properties: Map<String, String>,
        ): DatabaseSettings? {
            val rawUrl = value("SUPABASE_DATABASE_URL", environment, properties)
                ?: value("DATABASE_URL", environment, properties)

            val username = value("SUPABASE_DB_USER", environment, properties)
                ?: value("DATABASE_USER", environment, properties)
            val password = value("SUPABASE_DB_PASSWORD", environment, properties)
                ?: value("DATABASE_PASSWORD", environment, properties)

            if (!rawUrl.isNullOrBlank()) {
                val parsed = parseDatabaseUrl(rawUrl, username, password)
                if (parsed != null) return parsed
            }

            val host = value("SUPABASE_DB_HOST", environment, properties)
            val port = value("SUPABASE_DB_PORT", environment, properties) ?: "5432"
            val database = value("SUPABASE_DB_NAME", environment, properties) ?: "postgres"

            return if (!host.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
                DatabaseSettings(
                    jdbcUrl = "jdbc:postgresql://$host:$port/$database?sslmode=require",
                    username = username,
                    password = password,
                )
            } else {
                null
            }
        }

        private fun value(
            key: String,
            environment: Map<String, String>,
            properties: Map<String, String>,
        ): String? = environment[key] ?: properties[key]

        private fun parseDatabaseUrl(
            rawUrl: String,
            explicitUsername: String?,
            explicitPassword: String?,
        ): DatabaseSettings? {
            val normalized = rawUrl.removePrefix("jdbc:")
            val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
            val scheme = if (normalized.startsWith("postgresql://")) "jdbc:postgresql://" else "jdbc:$normalized"
            val path = uri.path?.removePrefix("/")?.ifBlank { "postgres" } ?: "postgres"
            val query = uri.rawQuery?.let { "?$it" } ?: "?sslmode=require"
            val userInfoParts = uri.userInfo?.split(":", limit = 2).orEmpty()
            val username = explicitUsername ?: userInfoParts.getOrNull(0)
            val password = explicitPassword ?: userInfoParts.getOrNull(1)

            if (username.isNullOrBlank() || password.isNullOrBlank() || uri.host.isNullOrBlank()) return null

            return DatabaseSettings(
                jdbcUrl = "$scheme${uri.host}:${if (uri.port == -1) 5432 else uri.port}/$path$query",
                username = username,
                password = password,
            )
        }
    }
}
