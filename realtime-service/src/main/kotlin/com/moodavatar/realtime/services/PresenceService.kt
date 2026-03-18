package com.moodavatar.realtime.services

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.slf4j.LoggerFactory

class PresenceService(
    redisHost: String,
    redisPort: Int,
) {
    private val log = LoggerFactory.getLogger(PresenceService::class.java)
    private val client: RedisClient = RedisClient.create("redis://$redisHost:$redisPort")
    private val connection: StatefulRedisConnection<String, String> = client.connect()
    private val commands = connection.sync()
    private val keyPrefix = "presence:"
    private val ttlSeconds = 300L // 5 min heartbeat TTL

    fun setOnline(userId: String) {
        try {
            commands.setex("$keyPrefix$userId", ttlSeconds, "1")
        } catch (e: Exception) {
            log.warn("Redis setOnline failed for $userId: ${e.message}")
        }
    }

    fun setOffline(userId: String) {
        try {
            commands.del("$keyPrefix$userId")
        } catch (e: Exception) {
            log.warn("Redis setOffline failed for $userId: ${e.message}")
        }
    }

    fun isOnline(userId: String): Boolean =
        try {
            commands.exists("$keyPrefix$userId") > 0
        } catch (e: Exception) {
            log.warn("Redis isOnline failed: ${e.message}")
            // Fall back to in-memory
            ConnectionManager.isOnline(userId)
        }

    fun heartbeat(userId: String) {
        try {
            commands.expire("$keyPrefix$userId", ttlSeconds)
        } catch (e: Exception) {
            log.warn("Redis heartbeat failed for $userId: ${e.message}")
        }
    }

    fun close() {
        connection.close()
        client.shutdown()
    }
}
