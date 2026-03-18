package com.moodavatar.realtime.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── Inbound (client → server) ─────────────────────────────────────────────────

@Serializable
data class InboundMessage(
    val type: String,
    val payload: JsonElement? = null,
)

// ── Outbound (server → client) ────────────────────────────────────────────────

@Serializable
data class FriendMoodUpdate(
    val type: String = "friend_mood_update",
    val userId: String,
    val username: String,
    val emotion: String,
    val intensity: Int,
    val note: String? = null,
)

@Serializable
data class FriendOnlineStatus(
    val type: String = "friend_online",
    val userId: String,
    val username: String,
    val online: Boolean,
)

@Serializable
data class ErrorMessage(
    val type: String = "error",
    val message: String,
)

@Serializable
data class PongMessage(
    val type: String = "pong",
)

// ── Room messages ──────────────────────────────────────────────────────────────

@Serializable
data class RoomVisitorInfo(
    val userId: String,
    val username: String,
    val emotion: String? = null,
    val skinColor: String? = null,
    val clothesColor: String? = null,
    val hairStyle: String? = null,
    val hairColor: String? = null,
)

/** Sent to the joining visitor: current state of the room */
@Serializable
data class RoomStateMessage(
    val type: String = "room_state",
    val visitors: List<RoomVisitorInfo>,
)

/** Broadcast to room owner + existing visitors when someone enters */
@Serializable
data class VisitorEntered(
    val type: String = "visitor_entered",
    val userId: String,
    val username: String,
    val emotion: String? = null,
    val skinColor: String? = null,
    val clothesColor: String? = null,
    val hairStyle: String? = null,
    val hairColor: String? = null,
)

/** Broadcast to room owner + remaining visitors when someone leaves */
@Serializable
data class VisitorLeft(
    val type: String = "visitor_left",
    val userId: String,
    val username: String,
)

/** Broadcast to everyone in the room when someone sends a reaction */
@Serializable
data class RoomReactionReceived(
    val type: String = "room_reaction_received",
    val fromUserId: String,
    val fromUsername: String,
    val reaction: String,
)

/** Sent to room owner when someone knocks */
@Serializable
data class RoomKnocked(
    val type: String = "room_knocked",
    val fromUserId: String,
    val fromUsername: String,
)
