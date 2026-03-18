package com.moodavatar.avatar.dto

import kotlinx.serialization.Serializable

@Serializable
data class SetMoodRequest(
    val emotion: String, // Enum-Name: HAPPY, SAD, …
    val intensity: Int, // 1–10
    val note: String? = null, // Optionaler Kommentar
)

@Serializable
data class MoodEntryResponse(
    val emotion: String,
    val intensity: Int,
    val note: String?,
    val setAt: String, // ISO-8601
)

@Serializable
data class AvatarConfigResponse(
    val primaryColor: String,
    val expression: String,
    val aura: String,
    val secondaryColor: String,
    val hairStyle: String,
    val accessories: List<String>,
    val skinColor: String,
    val clothesColor: String,
    val roomWallColor: String,
    val roomFloorColor: String,
    val roomItems: List<String>,
)

@Serializable
data class UpdateConfigRequest(
    val secondaryColor: String? = null,
    val hairStyle: String? = null,
    val accessories: List<String>? = null,
    val skinColor: String? = null,
    val clothesColor: String? = null,
    val roomWallColor: String? = null,
    val roomFloorColor: String? = null,
    val roomItems: List<String>? = null,
)

@Serializable
data class AvatarResponse(
    val userId: String,
    val currentMood: MoodEntryResponse?,
    val config: AvatarConfigResponse,
    val updatedAt: String,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)

// ── Admin / Mood-Monitoring DTOs ──────────────────────────────────────────────

@Serializable
data class EmotionCount(
    val emotion: String,
    val count: Long,
)

@Serializable
data class RecentMoodEntry(
    val userId: String,
    val emotion: String,
    val intensity: Int,
    val setAt: String,
)

@Serializable
data class MoodAdminStatsResponse(
    val emotionDistribution: List<EmotionCount>,
    val totalMoodEntries: Long,
    val activeUsers: Long,
    val averageIntensity: Double,
    val recentMoods: List<RecentMoodEntry>,
)
