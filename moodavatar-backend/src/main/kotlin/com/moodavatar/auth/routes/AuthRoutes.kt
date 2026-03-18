package com.moodavatar.auth.routes

import com.moodavatar.auth.dto.*
import com.moodavatar.auth.services.AuthService
import com.moodavatar.auth.services.EmailService
import com.moodavatar.users.dto.CreateProfileRequest
import com.moodavatar.users.services.ProfileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes(
    authService: AuthService,
    emailService: EmailService,
    profileService: ProfileService,
) {
    route("/auth") {
        get("/health") {
            call.respond(HttpStatusCode.OK, MessageResponse("ok"))
        }

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

            // Direct call instead of HTTP to user-service
            try {
                profileService.createProfile(
                    CreateProfileRequest(id = user.id, username = user.username, displayName = user.username)
                )
            } catch (_: Exception) {}

            try {
                val verificationToken = authService.createVerificationToken(UUID.fromString(user.id))
                emailService.sendEmailVerification(user.email, verificationToken)
            } catch (_: Exception) {}

            call.respond(HttpStatusCode.Created, user)
        }

        post("/login") {
            val req = call.receive<LoginRequest>()
            runCatching { authService.login(req) }
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_CREDENTIALS", "Wrong email or password"))
                }
        }

        post("/refresh") {
            val req = call.receive<RefreshRequest>()
            runCatching { authService.refresh(req) }
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse(it.message ?: "ERROR", "Token refresh failed"))
                }
        }

        post("/logout") {
            val req = call.receive<RefreshRequest>()
            authService.logout(req.refreshToken)
            call.respond(HttpStatusCode.OK, MessageResponse("Logged out successfully"))
        }

        post("/forgot-password") {
            val req = call.receive<ForgotPasswordRequest>()
            val result = authService.createPasswordResetToken(req.email)
            result?.let { (email, token) -> emailService.sendPasswordReset(email, token) }
            call.respond(HttpStatusCode.OK, MessageResponse("If the email exists, a reset link was sent."))
        }

        post("/reset-password") {
            val req = call.receive<ResetPasswordRequest>()
            runCatching { authService.resetPassword(req.token, req.newPassword) }
                .onSuccess { call.respond(HttpStatusCode.OK, MessageResponse("Password updated successfully")) }
                .onFailure {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "ERROR", "Password reset failed"))
                }
        }

        post("/verify-email") {
            val req = call.receive<VerifyEmailRequest>()
            runCatching { authService.verifyEmail(req.token) }
                .onSuccess { call.respond(HttpStatusCode.OK, MessageResponse("Email verified successfully")) }
                .onFailure {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "ERROR", "Email verification failed"))
                }
        }

        post("/resend-verification") {
            val req = call.receive<ResendVerificationRequest>()
            val pair = authService.resendVerification(req.email)
            if (pair != null) {
                emailService.sendEmailVerification(req.email, pair.second)
            }
            call.respond(HttpStatusCode.OK, MessageResponse("If the email exists and is unverified, a new link was sent."))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val user = authService.getUserById(UUID.fromString(userId))
                    ?: return@get call.respond(HttpStatusCode.NotFound)

                call.respond(HttpStatusCode.OK, user)
            }
        }
    }
}
