package com.moodavatar.auth.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.moodavatar.auth.dto.*
import com.moodavatar.auth.models.*
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.*

class AuthService(
    private val cfg: ApplicationConfig,
) {
    private val secret = cfg.property("jwt.secret").getString()
    private val issuer = cfg.property("jwt.issuer").getString()
    private val audience = cfg.property("jwt.audience").getString()
    private val accessExpiry = cfg.property("jwt.accessExpiryMs").getString().toLong()
    private val refreshExpiry = cfg.property("jwt.refreshExpiryMs").getString().toLong()

    fun register(req: RegisterRequest): UserResponse =
        transaction {
            if (Users.select { Users.email eq req.email }.count() > 0) error("EMAIL_TAKEN")
            if (Users.select { Users.username eq req.username }.count() > 0) error("USERNAME_TAKEN")

            val role = runCatching { Role.valueOf(req.role.uppercase()) }.getOrDefault(Role.USER)
            val now = LocalDateTime.now()
            val newId = UUID.randomUUID()

            Users.insert {
                it[id] = newId
                it[email] = req.email
                it[username] = req.username
                it[passwordHash] = BCrypt.hashpw(req.password, BCrypt.gensalt(12))
                it[Users.role] = role
                it[createdAt] = now
                it[updatedAt] = now
            }

            UserResponse(newId.toString(), req.email, req.username, role.name, isVerified = false)
        }

    fun login(req: LoginRequest): AuthResponse =
        transaction {
            val row =
                Users.select { Users.email eq req.email }.singleOrNull()
                    ?: error("INVALID_CREDENTIALS")

            if (!BCrypt.checkpw(req.password, row[Users.passwordHash])) error("INVALID_CREDENTIALS")

            val userId = row[Users.id]
            val userRole = row[Users.role].name
            val usernameVal = row[Users.username]

            AuthResponse(
                accessToken = generateAccessToken(userId, userRole, usernameVal),
                refreshToken = generateRefreshToken(userId),
                user =
                    UserResponse(
                        id = userId.toString(),
                        email = row[Users.email],
                        username = row[Users.username],
                        role = userRole,
                        isVerified = row[Users.isVerified],
                    ),
            )
        }

    fun refresh(req: RefreshRequest): RefreshResponse =
        transaction {
            val row =
                RefreshTokens
                    .select { RefreshTokens.token eq req.refreshToken }
                    .singleOrNull() ?: error("INVALID_REFRESH_TOKEN")

            if (row[RefreshTokens.revoked]) error("TOKEN_REVOKED")
            if (row[RefreshTokens.expiresAt].isBefore(LocalDateTime.now())) error("TOKEN_EXPIRED")

            val userId = row[RefreshTokens.userId]
            val userRow = Users.select { Users.id eq userId }.single()
            val userRole = userRow[Users.role].name
            val usernameVal = userRow[Users.username]

            RefreshResponse(accessToken = generateAccessToken(userId, userRole, usernameVal))
        }

    fun logout(refreshToken: String) =
        transaction {
            RefreshTokens.update({ RefreshTokens.token eq refreshToken }) {
                it[revoked] = true
            }
        }

    fun createPasswordResetToken(email: String): Pair<String, String>? =
        transaction {
            val row = Users.select { Users.email eq email }.singleOrNull() ?: return@transaction null
            val userId = row[Users.id]
            val resetToken = UUID.randomUUID().toString()
            val now = LocalDateTime.now()

            PasswordResets.insert {
                it[PasswordResets.userId] = userId
                it[token] = resetToken
                it[expiresAt] = now.plusHours(1)
            }

            Pair(row[Users.email], resetToken)
        }

    fun resetPassword(
        token: String,
        newPassword: String,
    ) = transaction {
        val row =
            PasswordResets
                .select { PasswordResets.token eq token }
                .singleOrNull() ?: error("INVALID_TOKEN")

        if (row[PasswordResets.used]) error("TOKEN_ALREADY_USED")
        if (row[PasswordResets.expiresAt].isBefore(LocalDateTime.now())) error("TOKEN_EXPIRED")

        val userId = row[PasswordResets.userId]
        Users.update({ Users.id eq userId }) {
            it[passwordHash] = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))
            it[updatedAt] = LocalDateTime.now()
        }
        PasswordResets.update({ PasswordResets.token eq token }) {
            it[used] = true
        }
    }

    fun getUserById(id: UUID): UserResponse? =
        transaction {
            Users.select { Users.id eq id }.singleOrNull()?.let {
                UserResponse(
                    id = it[Users.id].toString(),
                    email = it[Users.email],
                    username = it[Users.username],
                    role = it[Users.role].name,
                    isVerified = it[Users.isVerified],
                )
            }
        }

    fun createVerificationToken(userId: UUID): String =
        transaction {
            val verificationToken = UUID.randomUUID().toString()
            val now = LocalDateTime.now()
            EmailVerifications.insert {
                it[EmailVerifications.userId] = userId
                it[EmailVerifications.token] = verificationToken
                it[expiresAt] = now.plusHours(24)
            }
            verificationToken
        }

    fun verifyEmail(token: String) =
        transaction {
            val row =
                EmailVerifications
                    .select { EmailVerifications.token eq token }
                    .singleOrNull() ?: error("INVALID_TOKEN")

            if (row[EmailVerifications.used]) error("TOKEN_ALREADY_USED")
            if (row[EmailVerifications.expiresAt].isBefore(LocalDateTime.now())) error("TOKEN_EXPIRED")

            val userId = row[EmailVerifications.userId]
            Users.update({ Users.id eq userId }) {
                it[isVerified] = true
                it[updatedAt] = LocalDateTime.now()
            }
            EmailVerifications.update({ EmailVerifications.token eq token }) {
                it[used] = true
            }
        }

    fun resendVerification(email: String): Pair<UUID, String>? =
        transaction {
            val row = Users.select { Users.email eq email }.singleOrNull() ?: return@transaction null
            if (row[Users.isVerified]) return@transaction null
            val userId = row[Users.id]
            val token = UUID.randomUUID().toString()
            val now = LocalDateTime.now()
            EmailVerifications.insert {
                it[EmailVerifications.userId] = userId
                it[EmailVerifications.token] = token
                it[expiresAt] = now.plusHours(24)
            }
            Pair(userId, token)
        }

    private fun generateAccessToken(
        userId: UUID,
        role: String,
        username: String,
    ): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId.toString())
            .withClaim("role", role)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + accessExpiry))
            .sign(Algorithm.HMAC256(secret))

    private fun generateRefreshToken(userId: UUID): String {
        val token = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        transaction {
            RefreshTokens.insert {
                it[RefreshTokens.userId] = userId
                it[RefreshTokens.token] = token
                it[expiresAt] = now.plusSeconds(refreshExpiry / 1000)
                it[createdAt] = now
            }
        }
        return token
    }
}
