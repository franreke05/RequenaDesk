package com.requena.supportdesk.server.data.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.requena.supportdesk.server.config.DatabaseSettings
import java.sql.Connection

class PostgresSupportDeskDataSource(
    private val settings: DatabaseSettings,
) {
    private val hikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = settings.jdbcUrl
            username = settings.username
            password = settings.password
            maximumPoolSize = settings.maximumPoolSize
            minimumIdle = 1
            connectionTimeout = settings.connectionTimeoutMillis
            validationTimeout = settings.validationTimeoutMillis
            idleTimeout = settings.idleTimeoutMillis
            maxLifetime = settings.maxLifetimeMillis
            poolName = "orykai-supportdesk-db"
            connectionInitSql = "SET statement_timeout = '10s'"
            leakDetectionThreshold = 30_000
            addDataSourceProperty("ApplicationName", "orykai-supportdesk-server")
            addDataSourceProperty("reWriteBatchedInserts", "true")
        },
    )

    fun <T> withConnection(block: (Connection) -> T): T =
        hikariDataSource.connection.use(block)
}
