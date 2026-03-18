package com.moodavatar.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val role: String = "USER",
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val role: String,
    val isVerified: Boolean = false,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class RefreshResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)

@Serializable
data class VerifyEmailRequest(
    val token: String,
)

@Serializable
data class ResendVerificationRequest(
    val email: String,
)

// ── Admin DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class AdminUserResponse(
    val id: String,
    val email: String,
    val username: String,
    val role: String,
    val isVerified: Boolean,
    val createdAt: String,
)

@Serializable
data class AdminUsersResponse(
    val users: List<AdminUserResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

@Serializable
data class AdminStatsResponse(
    val totalUsers: Long,
    val usersToday: Long,
    val usersThisWeek: Long,
    val adminCount: Long,
    val verifiedCount: Long,
)

@Serializable
data class UpdateRoleRequest(
    val role: String,
)
