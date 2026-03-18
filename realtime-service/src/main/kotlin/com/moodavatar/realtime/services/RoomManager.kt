package com.moodavatar.realtime.services

import java.util.concurrent.ConcurrentHashMap

data class RoomVisitor(
    val userId: String,
    val username: String,
    val emotion: String? = null,
    val skinColor: String? = null,
    val clothesColor: String? = null,
    val hairStyle: String? = null,
    val hairColor: String? = null,
)

object RoomManager {
    // roomOwnerId -> (visitorUserId -> RoomVisitor)
    private val rooms = ConcurrentHashMap<String, ConcurrentHashMap<String, RoomVisitor>>()

    fun joinRoom(roomOwnerId: String, visitor: RoomVisitor) {
        rooms.computeIfAbsent(roomOwnerId) { ConcurrentHashMap() }[visitor.userId] = visitor
    }

    fun leaveRoom(roomOwnerId: String, visitorUserId: String): RoomVisitor? {
        val room = rooms[roomOwnerId] ?: return null
        val visitor = room.remove(visitorUserId)
        if (room.isEmpty()) rooms.remove(roomOwnerId)
        return visitor
    }

    fun getVisitors(roomOwnerId: String): List<RoomVisitor> =
        rooms[roomOwnerId]?.values?.toList() ?: emptyList()

    /** Remove user from every room they're visiting. Returns list of (ownerId, visitor) pairs. */
    fun leaveAllRooms(userId: String): List<Pair<String, RoomVisitor>> {
        val results = mutableListOf<Pair<String, RoomVisitor>>()
        for ((ownerId, room) in rooms) {
            val visitor = room.remove(userId) ?: continue
            results.add(ownerId to visitor)
            if (room.isEmpty()) rooms.remove(ownerId)
        }
        return results
    }
}
