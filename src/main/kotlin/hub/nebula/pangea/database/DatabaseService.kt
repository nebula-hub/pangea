package hub.nebula.pangea.database

import com.zaxxer.hikari.*
import hub.nebula.pangea.configuration.GeneralConfig.GalaxyConfig
import hub.nebula.pangea.database.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseService {
    fun connect(galaxy: GalaxyConfig) {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${galaxy.host}:${galaxy.port}/${galaxy.database}"
            driverClassName = "org.postgresql.Driver"
            username = galaxy.user
            password = galaxy.password
            maximumPoolSize = 8
        }

        Database.connect(
            HikariDataSource(config)
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Guilds,
                Songs,
                Users
            )
        }
    }
}