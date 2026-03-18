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
    val status: String,
    val senderUsername: String? = null,
)

@Serializable
data class FriendRequestAction(
    val action: String  // "ACCEPT" oder "DECLINE"
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class ErrorResponse(val error: String, val message: String)
