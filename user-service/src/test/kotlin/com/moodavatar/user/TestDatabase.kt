package com.moodavatar.user

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Singleton Testcontainers PostgreSQL instance shared across all user-service tests.
 */
object TestDatabase {

    val container: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
        withDatabaseName("moodavatar_user_test")
        withUsername("testuser")
        withPassword("testpass")
    }

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

        Runtime.getRuntime().addShutdownHook(Thread { container.stop() })
    }
}
