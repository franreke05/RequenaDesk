package com.requena.supportdesk.server.data.datasource

import com.requena.supportdesk.server.config.DatabaseSettings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

class PostgresSupportDeskDataSource(
    settings: DatabaseSettings,
) {
    private val pool: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = settings.jdbcUrl
            username = settings.username
            password = settings.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
        },
    )

    fun <T> withConnection(block: (Connection) -> T): T =
        pool.connection.use(block)
}
