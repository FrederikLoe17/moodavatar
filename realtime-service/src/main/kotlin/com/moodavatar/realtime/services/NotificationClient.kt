package com.moodavatar.realtime.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("NotificationClient")

@Serializable
data class CreateNotificationRequest(
    val userId: String,
    val type: String,
    val fromUserId: String,
    val fromUsername: String,
)

class NotificationClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun createNotification(userId: String, type: String, fromUserId: String, fromUsername: String) {
        try {
            httpClient.post("$baseUrl/notifications/internal/create") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(CreateNotificationRequest.serializer(),
                    CreateNotificationRequest(userId, type, fromUserId, fromUsername)))
            }
        } catch (e: Exception) {
            log.warn("Failed to create notification for $userId: ${e.message}")
        }
    }
}
