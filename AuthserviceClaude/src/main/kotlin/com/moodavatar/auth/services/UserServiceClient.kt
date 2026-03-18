package com.moodavatar.auth.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import org.slf4j.LoggerFactory

class UserServiceClient(
    private val http: HttpClient,
    private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(UserServiceClient::class.java)

    suspend fun createProfile(
        userId: String,
        username: String,
    ) {
        try {
            val body = """{"id":"$userId","username":"$username","displayName":"$username"}"""
            http.post("$baseUrl/users/internal/profile") {
                setBody(TextContent(body, ContentType.Application.Json))
            }
            log.info("Profile created for $username ($userId)")
        } catch (e: Exception) {
            log.error("Failed to create profile for $userId ($username): ${e.message}")
        }
    }
}
