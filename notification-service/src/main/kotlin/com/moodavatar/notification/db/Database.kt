package com.moodavatar.notification.db

import com.moodavatar.notification.models.Notifications
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabase() {
    val host     = environment.config.property("database.host").getString()
    val port     = environment.config.property("database.port").getString()
    val name     = environment.config.property("database.name").getString()
    val user     = environment.config.property("database.user").getString()
    val password = environment.config.property("database.password").getString()

    val url = "jdbc:postgresql://$host:$port/$name"

    Flyway.configure()
        .dataSource(url, user, password)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    val ds = HikariDataSource(HikariConfig().apply {
        jdbcUrl         = url
        username        = user
        this.password   = password
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
    })

    Database.connect(ds)
}
