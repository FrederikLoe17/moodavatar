package com.moodavatar.realtime.services

import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

data class ConnectedUser(
    val userId: String,
    val username: String,
    val session: WebSocketSession,
)

object ConnectionManager {
    private val log         = LoggerFactory.getLogger(ConnectionManager::class.java)
    private val connections = ConcurrentHashMap<String, ConnectedUser>()

    fun connect(user: ConnectedUser) {
        connections[user.userId] = user
        log.info("User connected: ${user.username} (${user.userId}). Total: ${connections.size}")
    }

    fun disconnect(userId: String) {
        connections.remove(userId)
        log.info("User disconnected: $userId. Total: ${connections.size}")
    }

    fun isOnline(userId: String): Boolean = connections.containsKey(userId)

    fun getOnlineUserIds(): Set<String> = connections.keys.toSet()

    suspend fun sendTo(userId: String, payload: String): Boolean {
        val conn = connections[userId] ?: return false
        return try {
            conn.session.send(Frame.Text(payload))
            true
        } catch (e: Exception) {
            log.warn("Failed to send to $userId: ${e.message}")
            connections.remove(userId)
            false
        }
    }

    suspend fun broadcastToUsers(userIds: List<String>, payload: String) {
        userIds.forEach { sendTo(it, payload) }
    }

    inline fun <reified T> encode(obj: T): String = Json.encodeToString(obj)
}
