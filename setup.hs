#!/usr/bin/env bash
set -e

ROOT="user-service"
PKG="src/main/kotlin/com/moodavatar/user"
RES="src/main/resources"

echo "🎭 MoodAvatar – User Service Setup"
echo "==================================="
mkdir -p "$ROOT"
cd "$ROOT"

# ── Ordnerstruktur ────────────────────────────────────────
mkdir -p "$PKG/plugins"
mkdir -p "$PKG/routes"
mkdir -p "$PKG/services"
mkdir -p "$PKG/models"
mkdir -p "$PKG/dto"
mkdir -p "$PKG/utils"
mkdir -p "$RES/db/migration"
mkdir -p "src/test/kotlin/com/moodavatar/user"

echo "📁 Ordnerstruktur erstellt"

# ── settings.gradle.kts ──────────────────────────────────
cat > settings.gradle.kts << 'EOF'
rootProject.name = "user-service"
EOF

# ── build.gradle.kts ─────────────────────────────────────
cat > build.gradle.kts << 'EOF'
val ktor_version = "2.3.7"
val kotlin_version = "1.9.22"
val exposed_version = "0.44.1"
val logback_version = "1.4.14"

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.moodavatar"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.4.1")
    implementation("org.flywaydb:flyway-database-postgresql:10.4.1")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}
EOF

# ── Dockerfile ───────────────────────────────────────────
cat > Dockerfile << 'EOF'
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# ── docker-compose.yml ───────────────────────────────────
cat > docker-compose.yml << 'EOF'
services:
  postgres-user:
    image: postgres:16-alpine
    container_name: moodavatar-user-postgres
    environment:
      POSTGRES_DB: moodavatar_user
      POSTGRES_USER: moodavatar
      POSTGRES_PASSWORD: secret123
    ports:
      - "5433:5432"
    volumes:
      - postgres_user_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U moodavatar -d moodavatar_user"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 10s

  user-service:
    build: .
    container_name: moodavatar-user
    ports:
      - "8082:8082"
    environment:
      DB_HOST: postgres-user
      DB_PORT: 5432
      DB_NAME: moodavatar_user
      DB_USER: moodavatar
      DB_PASSWORD: secret123
      JWT_SECRET: super-secret-jwt-key-change-in-production
      JWT_ISSUER: moodavatar
      JWT_AUDIENCE: moodavatar-users
      AUTH_SERVICE_URL: http://host.docker.internal:8081
    depends_on:
      postgres-user:
        condition: service_healthy

volumes:
  postgres_user_data:
EOF

# ── application.conf ─────────────────────────────────────
cat > "$RES/application.conf" << 'EOF'
ktor {
    deployment {
        port = 8082
    }
    application {
        modules = [ com.moodavatar.user.ApplicationKt.module ]
    }
}

jwt {
    secret = ${JWT_SECRET}
    issuer = ${JWT_ISSUER}
    audience = ${JWT_AUDIENCE}
}

database {
    host = ${DB_HOST}
    port = ${DB_PORT}
    name = ${DB_NAME}
    user = ${DB_USER}
    password = ${DB_PASSWORD}
}

services {
    authUrl = ${AUTH_SERVICE_URL}
}
EOF

# ── logback.xml ───────────────────────────────────────────
cat > "$RES/logback.xml" << 'EOF'
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <logger name="com.moodavatar" level="INFO"/>
    <logger name="io.ktor" level="WARN"/>
    <logger name="Exposed" level="WARN"/>
    <logger name="org.flywaydb" level="INFO"/>
    <logger name="com.zaxxer.hikari" level="WARN"/>
    <root level="WARN">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
EOF

# ── SQL Migrations ────────────────────────────────────────
cat > "$RES/db/migration/V1__create_profiles.sql" << 'EOF'
CREATE TABLE profiles (
    id           UUID PRIMARY KEY,
    username     VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    bio          TEXT,
    avatar_url   VARCHAR(512),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profiles_username ON profiles(username);
EOF

cat > "$RES/db/migration/V2__create_friendships.sql" << 'EOF'
CREATE TYPE friendship_status AS ENUM ('PENDING', 'ACCEPTED', 'DECLINED');

CREATE TABLE friend_requests (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id   UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    status      friendship_status NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(sender_id, receiver_id)
);

CREATE INDEX idx_friend_requests_sender   ON friend_requests(sender_id);
CREATE INDEX idx_friend_requests_receiver ON friend_requests(receiver_id);
EOF

# ── Application.kt ────────────────────────────────────────
cat > "$PKG/Application.kt" << 'EOF'
package com.moodavatar.user

import com.moodavatar.user.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureDatabase()
    configureSecurity()
    configureSerialization()
    configureRouting()
}
EOF

# ── plugins/Database.kt ───────────────────────────────────
cat > "$PKG/plugins/Database.kt" << 'EOF'
package com.moodavatar.user.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

fun Application.configureDatabase() {
    val cfg      = environment.config
    val host     = cfg.property("database.host").getString()
    val port     = cfg.property("database.port").getString()
    val name     = cfg.property("database.name").getString()
    val user     = cfg.property("database.user").getString()
    val password = cfg.property("database.password").getString()
    val jdbcUrl  = "jdbc:postgresql://$host:$port/$name"

    Flyway.configure()
        .dataSource(jdbcUrl, user, password)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(HikariDataSource(HikariConfig().apply {
        this.jdbcUrl        = jdbcUrl
        this.username       = user
        this.password       = password
        driverClassName     = "org.postgresql.Driver"
        maximumPoolSize     = 10
    }))
}
EOF

# ── plugins/Security.kt ───────────────────────────────────
cat > "$PKG/plugins/Security.kt" << 'EOF'
package com.moodavatar.user.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    val cfg      = environment.config
    val secret   = cfg.property("jwt.secret").getString()
    val issuer   = cfg.property("jwt.issuer").getString()
    val audience = cfg.property("jwt.audience").getString()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "moodavatar"
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
        }
    }
}
EOF

# ── plugins/Serialization.kt ──────────────────────────────
cat > "$PKG/plugins/Serialization.kt" << 'EOF'
package com.moodavatar.user.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = true })
    }
}
EOF

# ── plugins/Routing.kt ────────────────────────────────────
cat > "$PKG/plugins/Routing.kt" << 'EOF'
package com.moodavatar.user.plugins

import com.moodavatar.user.routes.friendRoutes
import com.moodavatar.user.routes.profileRoutes
import com.moodavatar.user.services.FriendService
import com.moodavatar.user.services.ProfileService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val profileService = ProfileService()
    val friendService  = FriendService()
    routing {
        profileRoutes(profileService)
        friendRoutes(friendService)
    }
}
EOF

# ── models/Tables.kt ─────────────────────────────────────
cat > "$PKG/models/Tables.kt" << 'EOF'
package com.moodavatar.user.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

enum class FriendshipStatus { PENDING, ACCEPTED, DECLINED }

object Profiles : Table("profiles") {
    val id          = uuid("id")
    val username    = varchar("username", 100).uniqueIndex()
    val displayName = varchar("display_name", 100).nullable()
    val bio         = text("bio").nullable()
    val avatarUrl   = varchar("avatar_url", 512).nullable()
    val createdAt   = datetime("created_at")
    val updatedAt   = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object FriendRequests : Table("friend_requests") {
    val id         = uuid("id").autoGenerate()
    val senderId   = uuid("sender_id").references(Profiles.id)
    val receiverId = uuid("receiver_id").references(Profiles.id)
    val status     = enumerationByName("status", 10, FriendshipStatus::class).default(FriendshipStatus.PENDING)
    val createdAt  = datetime("created_at")
    val updatedAt  = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}
EOF

# ── dto/UserDTOs.kt ───────────────────────────────────────
cat > "$PKG/dto/UserDTOs.kt" << 'EOF'
package com.moodavatar.user.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateProfileRequest(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val bio: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class ProfileResponse(
    val id: String,
    val username: String,
    val displayName: String?,
    val bio: String?,
    val avatarUrl: String?
)

@Serializable
data class FriendRequestResponse(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String
)

@Serializable
data class FriendRequestAction(
    val action: String  // "ACCEPT" oder "DECLINE"
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class ErrorResponse(val error: String, val message: String)
EOF

# ── services/ProfileService.kt ────────────────────────────
cat > "$PKG/services/ProfileService.kt" << 'EOF'
package com.moodavatar.user.services

import com.moodavatar.user.dto.*
import com.moodavatar.user.models.Profiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class ProfileService {

    fun createProfile(req: CreateProfileRequest): ProfileResponse = transaction {
        val id  = UUID.fromString(req.id)
        val now = LocalDateTime.now()
        Profiles.insert {
            it[Profiles.id]          = id
            it[username]             = req.username
            it[displayName]          = req.displayName
            it[bio]                  = null
            it[avatarUrl]            = null
            it[createdAt]            = now
            it[updatedAt]            = now
        }
        ProfileResponse(id.toString(), req.username, req.displayName, null, null)
    }

    fun getProfile(id: UUID): ProfileResponse? = transaction {
        Profiles.select { Profiles.id eq id }.singleOrNull()?.toResponse()
    }

    fun getProfileByUsername(username: String): ProfileResponse? = transaction {
        Profiles.select { Profiles.username eq username }.singleOrNull()?.toResponse()
    }

    fun searchProfiles(query: String): List<ProfileResponse> = transaction {
        Profiles.select { Profiles.username like "%$query%" }
            .limit(20)
            .map { it.toResponse() }
    }

    fun updateProfile(id: UUID, req: UpdateProfileRequest): ProfileResponse? = transaction {
        val updated = Profiles.update({ Profiles.id eq id }) {
            req.displayName?.let { v -> it[displayName] = v }
            req.bio?.let         { v -> it[bio]         = v }
            req.avatarUrl?.let   { v -> it[avatarUrl]   = v }
            it[updatedAt] = LocalDateTime.now()
        }
        if (updated == 0) null
        else Profiles.select { Profiles.id eq id }.single().toResponse()
    }

    private fun ResultRow.toResponse() = ProfileResponse(
        id          = this[Profiles.id].toString(),
        username    = this[Profiles.username],
        displayName = this[Profiles.displayName],
        bio         = this[Profiles.bio],
        avatarUrl   = this[Profiles.avatarUrl]
    )
}
EOF

# ── services/FriendService.kt ─────────────────────────────
cat > "$PKG/services/FriendService.kt" << 'EOF'
package com.moodavatar.user.services

import com.moodavatar.user.dto.*
import com.moodavatar.user.models.FriendRequests
import com.moodavatar.user.models.FriendshipStatus
import com.moodavatar.user.models.Profiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class FriendService {

    fun sendRequest(senderId: UUID, receiverId: UUID): FriendRequestResponse = transaction {
        if (senderId == receiverId) error("CANNOT_ADD_SELF")

        val exists = FriendRequests.select {
            (FriendRequests.senderId eq senderId) and
            (FriendRequests.receiverId eq receiverId) and
            (FriendRequests.status eq FriendshipStatus.PENDING)
        }.count() > 0
        if (exists) error("REQUEST_ALREADY_SENT")

        // Profil des Empfängers prüfen
        Profiles.select { Profiles.id eq receiverId }.singleOrNull()
            ?: error("USER_NOT_FOUND")

        val now = LocalDateTime.now()
        val id  = UUID.randomUUID()
        FriendRequests.insert {
            it[FriendRequests.id]         = id
            it[FriendRequests.senderId]   = senderId
            it[FriendRequests.receiverId] = receiverId
            it[status]                    = FriendshipStatus.PENDING
            it[createdAt]                 = now
            it[updatedAt]                 = now
        }
        FriendRequestResponse(id.toString(), senderId.toString(), receiverId.toString(), FriendshipStatus.PENDING.name)
    }

    fun respondToRequest(requestId: UUID, receiverId: UUID, action: String): FriendRequestResponse = transaction {
        val row = FriendRequests.select {
            (FriendRequests.id eq requestId) and
            (FriendRequests.receiverId eq receiverId)
        }.singleOrNull() ?: error("REQUEST_NOT_FOUND")

        if (row[FriendRequests.status] != FriendshipStatus.PENDING) error("REQUEST_ALREADY_HANDLED")

        val newStatus = when (action.uppercase()) {
            "ACCEPT"  -> FriendshipStatus.ACCEPTED
            "DECLINE" -> FriendshipStatus.DECLINED
            else      -> error("INVALID_ACTION")
        }

        FriendRequests.update({ FriendRequests.id eq requestId }) {
            it[status]    = newStatus
            it[updatedAt] = LocalDateTime.now()
        }

        FriendRequestResponse(
            requestId.toString(),
            row[FriendRequests.senderId].toString(),
            row[FriendRequests.receiverId].toString(),
            newStatus.name
        )
    }

    fun getFriends(userId: UUID): List<ProfileResponse> = transaction {
        val friendIds = FriendRequests.select {
            ((FriendRequests.senderId eq userId) or (FriendRequests.receiverId eq userId)) and
            (FriendRequests.status eq FriendshipStatus.ACCEPTED)
        }.map { row ->
            val sid = row[FriendRequests.senderId]
            val rid = row[FriendRequests.receiverId]
            if (sid == userId) rid else sid
        }

        Profiles.select { Profiles.id inList friendIds }.map {
            ProfileResponse(
                id          = it[Profiles.id].toString(),
                username    = it[Profiles.username],
                displayName = it[Profiles.displayName],
                bio         = it[Profiles.bio],
                avatarUrl   = it[Profiles.avatarUrl]
            )
        }
    }

    fun getPendingRequests(userId: UUID): List<FriendRequestResponse> = transaction {
        FriendRequests.select {
            (FriendRequests.receiverId eq userId) and
            (FriendRequests.status eq FriendshipStatus.PENDING)
        }.map {
            FriendRequestResponse(
                it[FriendRequests.id].toString(),
                it[FriendRequests.senderId].toString(),
                it[FriendRequests.receiverId].toString(),
                it[FriendRequests.status].name
            )
        }
    }

    fun removeFriend(userId: UUID, friendId: UUID): Boolean = transaction {
        val deleted = FriendRequests.deleteWhere {
            ((FriendRequests.senderId eq userId) and (FriendRequests.receiverId eq friendId) or
            (FriendRequests.senderId eq friendId) and (FriendRequests.receiverId eq userId)) and
            (FriendRequests.status eq FriendshipStatus.ACCEPTED)
        }
        deleted > 0
    }
}
EOF

# ── routes/ProfileRoutes.kt ───────────────────────────────
cat > "$PKG/routes/ProfileRoutes.kt" << 'EOF'
package com.moodavatar.user.routes

import com.moodavatar.user.dto.*
import com.moodavatar.user.services.ProfileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.profileRoutes(profileService: ProfileService) {
    route("/users") {

        // Interner Endpunkt: Profil anlegen (wird vom Auth Service nach Register aufgerufen)
        post("/internal/profile") {
            val req = call.receive<CreateProfileRequest>()
            runCatching { profileService.createProfile(req) }
                .onSuccess { call.respond(HttpStatusCode.Created, it) }
                .onFailure { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "ERROR", "Profile creation failed")) }
        }

        authenticate("auth-jwt") {

            // GET /users/me
            get("/me") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val profile = profileService.getProfile(userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Profile not found"))
                call.respond(HttpStatusCode.OK, profile)
            }

            // PUT /users/me
            put("/me") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val req    = call.receive<UpdateProfileRequest>()
                val updated = profileService.updateProfile(userId, req)
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK, updated)
            }

            // GET /users/search?q=username
            get("/search") {
                val query = call.request.queryParameters["q"]?.trim()
                if (query.isNullOrBlank())
                    return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("MISSING_QUERY", "Query parameter 'q' is required"))
                call.respond(HttpStatusCode.OK, profileService.searchProfiles(query))
            }

            // GET /users/{id}
            get("/{id}") {
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val profile = profileService.getProfile(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK, profile)
            }
        }
    }
}

fun ApplicationCall.userId(): UUID? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
EOF

# ── routes/FriendRoutes.kt ────────────────────────────────
cat > "$PKG/routes/FriendRoutes.kt" << 'EOF'
package com.moodavatar.user.routes

import com.moodavatar.user.dto.*
import com.moodavatar.user.services.FriendService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.friendRoutes(friendService: FriendService) {
    authenticate("auth-jwt") {
        route("/friends") {

            // GET /friends – Freundesliste
            get {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(HttpStatusCode.OK, friendService.getFriends(userId))
            }

            // GET /friends/requests – offene Anfragen
            get("/requests") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(HttpStatusCode.OK, friendService.getPendingRequests(userId))
            }

            // POST /friends/requests/{receiverId} – Anfrage senden
            post("/requests/{receiverId}") {
                val userId     = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val receiverId = call.parameters["receiverId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@post call.respond(HttpStatusCode.BadRequest)

                runCatching { friendService.sendRequest(userId, receiverId) }
                    .onSuccess { call.respond(HttpStatusCode.Created, it) }
                    .onFailure {
                        val msg = it.message ?: "ERROR"
                        val status = if (msg == "USER_NOT_FOUND") HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                        call.respond(status, ErrorResponse(msg, "Friend request failed"))
                    }
            }

            // PATCH /friends/requests/{requestId} – Anfrage annehmen/ablehnen
            patch("/requests/{requestId}") {
                val userId    = call.userId() ?: return@patch call.respond(HttpStatusCode.Unauthorized)
                val requestId = call.parameters["requestId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val body = call.receive<FriendRequestAction>()

                runCatching { friendService.respondToRequest(requestId, userId, body.action) }
                    .onSuccess { call.respond(HttpStatusCode.OK, it) }
                    .onFailure { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "ERROR", "Action failed")) }
            }

            // DELETE /friends/{friendId} – Freund entfernen
            delete("/{friendId}") {
                val userId   = call.userId() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val friendId = call.parameters["friendId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)

                val removed = friendService.removeFriend(userId, friendId)
                if (removed) call.respond(HttpStatusCode.OK, MessageResponse("Friend removed"))
                else call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Friendship not found"))
            }
        }
    }
}
EOF

echo ""
echo "✅ Fertig! Projekt wurde in ./$ROOT angelegt"
echo ""
echo "🚀 Starten mit:"
echo "   cd $ROOT"
echo "   docker compose up --build"
echo ""
echo "👤 User Service API: http://localhost:8082"