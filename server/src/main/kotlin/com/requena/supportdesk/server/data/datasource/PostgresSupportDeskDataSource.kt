package com.requena.supportdesk.server.data.datasource

import com.requena.supportdesk.server.config.DatabaseSettings
import java.sql.Connection
import java.sql.DriverManager

class PostgresSupportDeskDataSource(
    private val settings: DatabaseSettings,
) {
    init {
        Class.forName("org.postgresql.Driver")
    }

    fun <T> withConnection(block: (Connection) -> T): T =
        DriverManager.getConnection(settings.jdbcUrl, settings.username, settings.password).use(block)
}
