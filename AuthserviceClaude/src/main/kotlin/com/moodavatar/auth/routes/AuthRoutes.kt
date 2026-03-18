package com.moodavatar.auth.routes

import com.moodavatar.auth.dto.*
import com.moodavatar.auth.services.AuthService
import com.moodavatar.auth.services.EmailService
import com.moodavatar.auth.services.UserServiceClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes(authService: AuthService, emailService: EmailService, userServiceClient: UserServiceClient) {
    route("/auth") {

        // GET /auth/health
        get("/health") {
            call.respond(HttpStatusCode.OK, MessageResponse("ok"))
        }


        // POST /auth/register
        post("/register") {
            val req = call.receive<RegisterRequest>()
            val user = try {
                authService.register(req)
            } catch (e: Exception) {
                val msg = e.message ?: "UNKNOWN"
                val status = if (msg.contains("TAKEN")) HttpStatusCode.Conflict else HttpStatusCode.BadRequest
                call.respond(status, ErrorResponse(msg, "Registration failed"))
                return@post
            }
            try { userServiceClient.createProfile(user.id, user.username) } catch (_: Exception) {}
            try {
                val verificationToken = authService.createVerificationToken(UUID.fromString(user.id))
                emailService.sendEmailVerification(user.email, verificationToken)
            } catch (_: Exception) {}
            call.respond(HttpStatusCode.Created, user)
        }

        // POST /auth/login
        post("/login") {
            val req = call.receive<LoginRequest>()
            runCatching { authService.login(req) }
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_CREDENTIALS", "Wrong email or password"))
                }
        }

        // POST /auth/refresh
        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            runCatching { authService.refresh(req) }
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(it.message ?: "ERROR", "Token refresh failed"))
                }
        }

        // POST /auth/logout
        post("/logout") {
            val req = call.receive<RefreshRequest>()
            authService.logout(req.refreshToken)
            call.respond(HttpStatusCode.OK, MessageResponse("Logged out successfully"))
        }

        // POST /auth/forgot-password
        post("/forgot-password") {
            val req = call.receive<ForgotPasswordRequest>()
            val result = authService.createPasswordResetToken(req.email)
            result?.let { (email, token) ->
                emailService.sendPasswordReset(email, token)
            }
            // Immer gleiche Antwort (Security: kein Hinweis ob E-Mail existiert)
            call.respond(HttpStatusCode.OK, MessageResponse("If the email exists, a reset link was sent."))
        }

        // POST /auth/reset-password
        post("/reset-password") {
            val req = call.receive<ResetPasswordRequest>()
            runCatching { authService.resetPassword(req.token, req.newPassword) }
                .onSuccess { call.respond(HttpStatusCode.OK, MessageResponse("Password updated successfully")) }
                .onFailure {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "ERROR", "Password reset failed"))
                }
        }

        // POST /auth/verify-email
        post("/verify-email") {
            val req = call.receive<VerifyEmailRequest>()
            runCatching { authService.verifyEmail(req.token) }
                .onSuccess { call.respond(HttpStatusCode.OK, MessageResponse("Email verified successfully")) }
                .onFailure {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "ERROR", "Email verification failed"))
                }
        }

        // POST /auth/resend-verification
        post("/resend-verification") {
            val req = call.receive<ResendVerificationRequest>()
            val pair = authService.resendVerification(req.email)
            if (pair != null) {
                emailService.sendEmailVerification(req.email, pair.second)
            }
            call.respond(HttpStatusCode.OK, MessageResponse("If the email exists and is unverified, a new link was sent."))
        }

        // GET /auth/me  (JWT-geschützt)
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId    = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val user = authService.getUserById(UUID.fromString(userId))
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(HttpStatusCode.OK, user)
            }
        }
    }
}