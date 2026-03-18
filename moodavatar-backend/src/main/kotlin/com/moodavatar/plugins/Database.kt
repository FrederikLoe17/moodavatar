package com.moodavatar.plugins

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase(): MongoDatabase {
    // ── PostgreSQL ────────────────────────────────────────────────────────────
    val cfg = environment.config
    val host = cfg.property("database.host").getString()
    val port = cfg.property("database.port").getString()
    val name = cfg.property("database.name").getString()
    val user = cfg.property("database.user").getString()
    val password = cfg.property("database.password").getString()
    val jdbcUrl = "jdbc:postgresql://$host:$port/$name"

    Flyway
        .configure()
        .dataSource(jdbcUrl, user, password)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    val hikari = HikariDataSource(
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 15
        }
    )
    Database.connect(hikari)

    // ── MongoDB ───────────────────────────────────────────────────────────────
    val mongoUri = cfg.property("mongodb.uri").getString()
    val mongoDb  = cfg.property("mongodb.db").getString()
    val mongoClient: MongoClient = MongoClients.create(mongoUri)
    return mongoClient.getDatabase(mongoDb)
}
