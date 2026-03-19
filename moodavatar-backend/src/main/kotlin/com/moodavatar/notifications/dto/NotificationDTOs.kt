package com.moodavatar.notifications.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val fromUserId: String,
    val fromUsername: String,
    val read: Boolean,
    val metadata: String?,
    val createdAt: String,
)

@Serializable
data class UnreadCountResponse(
    val count: Int,
)

@Serializable
data class MessageResponse(
    val message: String,
)
