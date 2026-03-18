package com.moodavatar.realtime.services

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import org.slf4j.LoggerFactory

class PresenceService(redisHost: String, redisPort: Int) {
    private val log = LoggerFactory.getLogger(PresenceService::class.java)
    private val client: RedisClient = RedisClient.create("redis://$redisHost:$redisPort")
    private val connection: StatefulRedisConnection<String, String> = client.connect()
    private val commands = connection.sync()
    private val KEY_PREFIX = "presence:"
    private val TTL_SECONDS = 300L // 5 min heartbeat TTL

    fun setOnline(userId: String) {
        try {
            commands.setex("$KEY_PREFIX$userId", TTL_SECONDS, "1")
        } catch (e: Exception) {
            log.warn("Redis setOnline failed for $userId: ${e.message}")
        }
    }

    fun setOffline(userId: String) {
        try {
            commands.del("$KEY_PREFIX$userId")
        } catch (e: Exception) {
            log.warn("Redis setOffline failed for $userId: ${e.message}")
        }
    }

    fun isOnline(userId: String): Boolean {
        return try {
            commands.exists("$KEY_PREFIX$userId") > 0
        } catch (e: Exception) {
            log.warn("Redis isOnline failed: ${e.message}")
            // Fall back to in-memory
            ConnectionManager.isOnline(userId)
        }
    }

    fun heartbeat(userId: String) {
        try {
            commands.expire("$KEY_PREFIX$userId", TTL_SECONDS)
        } catch (e: Exception) {
            log.warn("Redis heartbeat failed for $userId: ${e.message}")
        }
    }

    fun close() {
        connection.close()
        client.shutdown()
    }
}
