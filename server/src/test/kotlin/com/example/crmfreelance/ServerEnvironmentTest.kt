package com.example.crmfreelance

import com.requena.supportdesk.server.config.ServerEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServerEnvironmentTest {
    @Test
    fun postgresAliasUrlIsConvertedToJdbcAndPoolSizeIsBounded() {
        val environment = ServerEnvironment.load(
            environment = mapOf(
                AUTH_SECRET_KEY to AUTH_SECRET,
                "DATABASE_URL" to "postgres://dbuser:p%40ssword@db.example.com:6543/postgres?sslmode=require",
                "SUPPORTDESK_DB_MAX_POOL_SIZE" to "7",
            ),
            properties = emptyMap(),
        )

        val database = assertNotNull(environment.database)
        assertEquals("jdbc:postgresql://db.example.com:6543/postgres?sslmode=require", database.jdbcUrl)
        assertEquals("dbuser", database.username)
        assertEquals("p@ssword", database.password)
        assertEquals(7, database.maximumPoolSize)
    }

    @Test
    fun incompleteDatabaseSettingsFailAtStartup() {
        val error = assertFailsWith<IllegalArgumentException> {
            ServerEnvironment.load(
                environment = mapOf(
                    AUTH_SECRET_KEY to AUTH_SECRET,
                    "DATABASE_USER" to "supportdesk",
                ),
                properties = emptyMap(),
            )
        }

        assertTrue(error.message.orEmpty().contains("Incomplete database configuration"))
    }

    @Test
    fun bootstrapRejectsWeakPasswords() {
        assertFailsWith<IllegalArgumentException> {
            ServerEnvironment.load(
                environment = mapOf(
                    AUTH_SECRET_KEY to AUTH_SECRET,
                    "SUPPORTDESK_BOOTSTRAP_DEMO_DATA" to "true",
                    "SUPPORTDESK_BOOTSTRAP_ADMIN_PASSWORD" to "short",
                    "SUPPORTDESK_BOOTSTRAP_CLIENT_PASSWORD" to "also-short",
                ),
                properties = emptyMap(),
            )
        }
    }

    private companion object {
        const val AUTH_SECRET_KEY = "SUPPORTDESK_AUTH_SECRET"
        const val AUTH_SECRET = "supportdesk-test-secret-1234567890"
    }
}
