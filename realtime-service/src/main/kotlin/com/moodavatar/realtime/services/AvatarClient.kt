package com.moodavatar.realtime.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AvatarClient")

class AvatarClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    suspend fun onSocialEvent(userId: String) {
        try {
            httpClient.post("$baseUrl/avatars/internal/needs/social?userId=$userId") {
                contentType(ContentType.Application.Json)
            }
        } catch (e: Exception) {
            log.warn("Failed to update social need for $userId: ${e.message}")
        }
    }
}
