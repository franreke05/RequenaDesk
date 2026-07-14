package com.requena.supportdesk.server.data.datasource

import com.requena.supportdesk.server.config.DatabaseSettings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.flywaydb.core.Flyway

class PostgresSupportDeskDataSource(
    settings: DatabaseSettings,
) : AutoCloseable {
    private val pool: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = settings.jdbcUrl
            username = settings.username
            password = settings.password
            driverClassName = "org.postgresql.Driver"
            poolName = "SupportDeskPool"
            maximumPoolSize = settings.maximumPoolSize
            minimumIdle = 1
            connectionTimeout = 10_000
            validationTimeout = 5_000
            idleTimeout = 600_000
            maxLifetime = 1_800_000
            keepaliveTime = 120_000
            initializationFailTimeout = 10_000

            if (settings.jdbcUrl.contains(":6543/")) {
                addDataSourceProperty("prepareThreshold", "0")
            }
        },
    )

    fun migrate(): Int = Flyway.configure()
        .dataSource(pool)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .baselineDescription("Existing SupportDesk schema")
        .load()
        .migrate()
        .migrationsExecuted

    fun isReady(): Boolean = runCatching {
        withConnection { connection ->
            connection.prepareStatement(
                """
                SELECT
                    to_regclass('public.users') IS NOT NULL
                    AND to_regclass('public.clients') IS NOT NULL
                    AND to_regclass('public.tickets') IS NOT NULL
                    AND to_regclass('public.invoices') IS NOT NULL
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { result -> result.next() && result.getBoolean(1) }
            }
        }
    }.getOrDefault(false)

    fun <T> withConnection(block: (Connection) -> T): T =
        pool.connection.use(block)

    override fun close() {
        pool.close()
    }
}
