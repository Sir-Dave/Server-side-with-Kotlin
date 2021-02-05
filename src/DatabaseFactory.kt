package com.sirdave

import com.sirdave.repository.Todos
import com.sirdave.repository.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object DatabaseFactory {
    private var host: String
    private var port: Int = 0
    private var databaseName : String
    private var databaseUser : String
    private var databasePassword : String

    init{
        //Database.connect(hikari())
        val dbUrl = System.getenv("DATABASE_URL")
        if (dbUrl != null){
            val dbUri = URI(dbUrl)
            host = dbUri.host
            port = dbUri.port
            databaseName = dbUri.path.substring(1)
            val userInfo = dbUri.userInfo.split(":")
            databaseUser = userInfo[0]
            databasePassword = userInfo[1]

        }
        else{
            host = System.getenv("DB_HOST")
            port = System.getenv("DB_PORT").toInt()
            databaseName = System.getenv("DB_NAME")
            databaseUser = System.getenv("DB_USER")
            databasePassword =System.getenv("DB_PASSWORD")

        }

        /**transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(Todos)
        }*/
    }
    fun connect(){
        Database.connect(
                "jdbc:postgresql://$host:$port/$databaseName",
                driver = "org.postgresql.Driver", user = databaseUser, password = databasePassword
        )

        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(Todos)
        }
    }
    private fun hikari(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = System.getenv("JDBC_DRIVER") // 1
        config.jdbcUrl = System.getenv("JDBC_DATABASE_URL") // 2
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        val user = System.getenv("DB_USER") // 3
        if (user != null) {
            config.username = user
        }
        val password = System.getenv("DB_PASSWORD") // 4
        if (password != null) {
            config.password = password
        }
        config.validate()
        return HikariDataSource(config)
    }

    // 5
    suspend fun <T> dbQuery(block: () -> T): T =
            withContext(Dispatchers.IO) {
                transaction { block() }
            }
}