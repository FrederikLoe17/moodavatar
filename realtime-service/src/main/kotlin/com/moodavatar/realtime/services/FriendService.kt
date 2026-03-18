package com.moodavatar.realtime.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class FriendProfile(
    val id: String,
    val username: String,
    val displayName: String? = null,
)

class FriendService(
    private val httpClient: HttpClient,
    private val userServiceUrl: String,
) {
    private val log = LoggerFactory.getLogger(FriendService::class.java)

    // Simple in-memory cache: userId → (friends, expiry)
    private val cache = ConcurrentHashMap<String, Pair<List<FriendProfile>, Long>>()
    private val cacheTtlMs = 5 * 60 * 1000L // 5 minutes

    suspend fun getFriendIds(
        userId: String,
        token: String,
    ): List<String> {
        val now = System.currentTimeMillis()
        val cached = cache[userId]
        if (cached != null && now < cached.second) {
            return cached.first.map { it.id }
        }
        return try {
            val friends =
                httpClient
                    .get("$userServiceUrl/friends") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }.body<List<FriendProfile>>()
            cache[userId] = Pair(friends, now + cacheTtlMs)
            friends.map { it.id }
        } catch (e: Exception) {
            log.warn("Failed to fetch friends for $userId: ${e.message}")
            emptyList()
        }
    }

    fun invalidateCache(userId: String) {
        cache.remove(userId)
    }
}
