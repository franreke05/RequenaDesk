package com.requena.supportdesk.server.config

import com.requena.supportdesk.server.security.SecurityConfig
import com.requena.supportdesk.server.security.ServerAuthSettings
import java.net.URI

data class DatabaseSettings(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int = 5,
)

data class ServerEnvironment(
    val database: DatabaseSettings?,
    val auth: ServerAuthSettings,
    val bootstrapDemoData: Boolean,
    val bootstrapAdminPassword: String?,
    val bootstrapClientPassword: String?,
) {
    companion object {
        fun load(
            environment: Map<String, String> = System.getenv(),
            properties: Map<String, String> = defaultServerProperties(),
        ): ServerEnvironment {
            val bootstrapDemoData = value("SUPPORTDESK_BOOTSTRAP_DEMO_DATA", environment, properties)?.toBooleanStrictOrNull() == true
            val bootstrapAdminPassword = value("SUPPORTDESK_BOOTSTRAP_ADMIN_PASSWORD", environment, properties)
            val bootstrapClientPassword = value("SUPPORTDESK_BOOTSTRAP_CLIENT_PASSWORD", environment, properties)
            if (bootstrapDemoData) {
                require(!bootstrapAdminPassword.isNullOrBlank()) {
                    "SUPPORTDESK_BOOTSTRAP_ADMIN_PASSWORD is required when SUPPORTDESK_BOOTSTRAP_DEMO_DATA=true."
                }
                require(!bootstrapClientPassword.isNullOrBlank()) {
                    "SUPPORTDESK_BOOTSTRAP_CLIENT_PASSWORD is required when SUPPORTDESK_BOOTSTRAP_DEMO_DATA=true."
                }
                require(bootstrapAdminPassword.length >= MINIMUM_BOOTSTRAP_PASSWORD_LENGTH) {
                    "SUPPORTDESK_BOOTSTRAP_ADMIN_PASSWORD must contain at least $MINIMUM_BOOTSTRAP_PASSWORD_LENGTH characters."
                }
                require(bootstrapClientPassword.length >= MINIMUM_BOOTSTRAP_PASSWORD_LENGTH) {
                    "SUPPORTDESK_BOOTSTRAP_CLIENT_PASSWORD must contain at least $MINIMUM_BOOTSTRAP_PASSWORD_LENGTH characters."
                }
            }
            return ServerEnvironment(
                database = resolveDatabaseSettings(environment, properties),
                auth = resolveAuthSettings(environment, properties),
                bootstrapDemoData = bootstrapDemoData,
                bootstrapAdminPassword = bootstrapAdminPassword,
                bootstrapClientPassword = bootstrapClientPassword,
            )
        }

        private fun defaultServerProperties(): Map<String, String> =
            loadServerProperties() + System.getProperties().stringPropertyNames().associateWith(System::getProperty)

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
                return requireNotNull(parseDatabaseUrl(rawUrl, username, password)) {
                    "Database URL is invalid or has no credentials. Use postgresql://user:password@host:port/database or provide DATABASE_USER and DATABASE_PASSWORD."
                }.copy(maximumPoolSize = resolveMaximumPoolSize(environment, properties))
            }

            val host = value("SUPABASE_DB_HOST", environment, properties)
            val port = value("SUPABASE_DB_PORT", environment, properties) ?: "5432"
            val database = value("SUPABASE_DB_NAME", environment, properties) ?: "postgres"

            val hasPartialSettings = listOf(host, username, password).any { !it.isNullOrBlank() }
            if (hasPartialSettings) {
                require(!host.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
                    "Incomplete database configuration. Host, user and password are required."
                }
            }

            val sslMode = value("SUPPORTDESK_DB_SSLMODE", environment, properties)
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: "require"

            return if (!host.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
                DatabaseSettings(
                    jdbcUrl = "jdbc:postgresql://$host:$port/$database?sslmode=$sslMode",
                    username = username,
                    password = password,
                    maximumPoolSize = resolveMaximumPoolSize(environment, properties),
                )
            } else {
                null
            }
        }

        private fun resolveMaximumPoolSize(
            environment: Map<String, String>,
            properties: Map<String, String>,
        ): Int = value("SUPPORTDESK_DB_MAX_POOL_SIZE", environment, properties)
            ?.toIntOrNull()
            ?.takeIf { it in 1..20 }
            ?: 5

        private fun resolveAuthSettings(
            environment: Map<String, String>,
            properties: Map<String, String>,
        ): ServerAuthSettings {
            val secret = value(SecurityConfig.authSecretKey, environment, properties)
                ?: value(SecurityConfig.legacyJwtSecretKey, environment, properties)
                ?: error(
                    "${SecurityConfig.authSecretKey} is required. Generate a long random secret before exposing the server to the internet.",
                )

            require(secret.length >= SecurityConfig.minimumSecretLength) {
                "${SecurityConfig.authSecretKey} must contain at least ${SecurityConfig.minimumSecretLength} characters."
            }

            val accessTokenLifetimeMinutes = value(SecurityConfig.accessTokenLifetimeMinutesKey, environment, properties)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
                ?: SecurityConfig.defaultAccessTokenLifetimeMinutes
            val refreshTokenLifetimeDays = value(SecurityConfig.refreshTokenLifetimeDaysKey, environment, properties)
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
                ?: SecurityConfig.defaultRefreshTokenLifetimeDays

            return ServerAuthSettings(
                secret = secret,
                issuer = value(SecurityConfig.authIssuerKey, environment, properties) ?: SecurityConfig.defaultIssuer,
                audience = value(SecurityConfig.authAudienceKey, environment, properties) ?: SecurityConfig.defaultAudience,
                accessTokenLifetimeMinutes = accessTokenLifetimeMinutes,
                refreshTokenLifetimeDays = refreshTokenLifetimeDays,
            )
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
            val normalized = rawUrl
                .trim()
                .removePrefix("jdbc:")
                .replaceFirst("postgres://", "postgresql://")
            if (!normalized.startsWith("postgresql://")) return null

            val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
            val path = uri.path?.removePrefix("/")?.ifBlank { "postgres" } ?: "postgres"
            val query = uri.rawQuery?.let { "?$it" } ?: "?sslmode=require"
            val userInfoParts = uri.userInfo?.split(":", limit = 2).orEmpty()
            val username = explicitUsername ?: userInfoParts.getOrNull(0)
            val password = explicitPassword ?: userInfoParts.getOrNull(1)

            if (username.isNullOrBlank() || password.isNullOrBlank() || uri.host.isNullOrBlank()) return null

            return DatabaseSettings(
                jdbcUrl = "jdbc:postgresql://${uri.host}:${if (uri.port == -1) 5432 else uri.port}/$path$query",
                username = username,
                password = password,
            )
        }

        private const val MINIMUM_BOOTSTRAP_PASSWORD_LENGTH = 12
    }
}
