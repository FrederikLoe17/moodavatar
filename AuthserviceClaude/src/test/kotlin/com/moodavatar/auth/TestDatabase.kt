package com.moodavatar.auth

import io.ktor.server.config.MapApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Singleton Testcontainers PostgreSQL instance shared across all auth-service tests.
 * Started once on first access, stopped via JVM shutdown hook.
 */
object TestDatabase {

    val container: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
        withDatabaseName("moodavatar_auth_test")
        withUsername("testuser")
        withPassword("testpass")
    }

    val config: MapApplicationConfig

    init {
        container.start()

        Flyway.configure()
            .dataSource(container.jdbcUrl, container.username, container.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        Database.connect(
            url = container.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = container.username,
            password = container.password,
        )

        config = MapApplicationConfig(
            "jwt.secret" to "test-secret-key-long-enough-for-hmac",
            "jwt.issuer" to "test-issuer",
            "jwt.audience" to "test-audience",
            "jwt.accessExpiryMs" to "900000",
            "jwt.refreshExpiryMs" to "2592000000",
            "smtp.host" to "localhost",
            "smtp.port" to "9999",
            "smtp.user" to "",
            "smtp.password" to "",
            "app.baseUrl" to "http://localhost:5173",
            "services.userServiceUrl" to "http://localhost:9999",
            "database.host" to container.host,
            "database.port" to container.firstMappedPort.toString(),
            "database.name" to container.databaseName,
            "database.user" to container.username,
            "database.password" to container.password,
        )

        Runtime.getRuntime().addShutdownHook(Thread { container.stop() })
    }
}
