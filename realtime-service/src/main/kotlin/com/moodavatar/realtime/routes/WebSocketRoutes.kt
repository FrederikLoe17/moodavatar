package com.moodavatar.realtime.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.moodavatar.realtime.models.*
import com.moodavatar.realtime.services.AvatarClient
import com.moodavatar.realtime.services.ConnectedUser
import com.moodavatar.realtime.services.ConnectionManager
import com.moodavatar.realtime.services.FriendService
import com.moodavatar.realtime.services.NotificationClient
import com.moodavatar.realtime.services.PresenceService
import com.moodavatar.realtime.services.RoomManager
import com.moodavatar.realtime.services.RoomVisitor
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("WebSocketRoutes")
private val json = Json { ignoreUnknownKeys = true }

fun Route.webSocketRoutes(
    jwtSecret: String,
    friendService: FriendService,
    presenceService: PresenceService,
    notificationClient: NotificationClient,
    avatarClient: AvatarClient,
) {
    val algorithm = Algorithm.HMAC256(jwtSecret)
    val verifier =
        JWT
            .require(algorithm)
            .withIssuer("moodavatar")
            .withAudience("moodavatar-users")
            .build()

    webSocket("/ws") {
        // Authenticate via query param (browser WS doesn't support custom headers)
        val token = call.request.queryParameters["token"]
        if (token == null) {
            send(json.encodeToString(ErrorMessage.serializer(), ErrorMessage(message = "Missing token")))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
            return@webSocket
        }

        val decodedJwt =
            try {
                verifier.verify(token)
            } catch (e: Exception) {
                send(json.encodeToString(ErrorMessage.serializer(), ErrorMessage(message = "Invalid token")))
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

        val userId = decodedJwt.getClaim("userId").asString()
        val username = decodedJwt.getClaim("username").asString() ?: userId
        if (userId == null) {
            send(ConnectionManager.encode(ErrorMessage(message = "userId claim missing")))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "userId claim missing"))
            return@webSocket
        }

        val user = ConnectedUser(userId, username, this)
        ConnectionManager.connect(user)
        presenceService.setOnline(userId)

        // Notify friends that this user came online
        launch {
            val friendIds = friendService.getFriendIds(userId, token)
            val msg = ConnectionManager.encode(FriendOnlineStatus(userId = userId, username = username, online = true))
            ConnectionManager.broadcastToUsers(friendIds, msg)
        }

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()

                val inbound =
                    try {
                        json.decodeFromString<InboundMessage>(text)
                    } catch (e: Exception) {
                        log.warn("Unparseable message from $userId: $text")
                        continue
                    }

                when (inbound.type) {
                    "ping" -> {
                        send(ConnectionManager.encode(PongMessage()))
                        presenceService.heartbeat(userId)
                    }

                    "mood_update" -> {
                        val payload = inbound.payload?.jsonObject ?: continue
                        val emotion = payload["emotion"]?.jsonPrimitive?.content ?: continue
                        val intensity = payload["intensity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 5
                        val note = payload["note"]?.jsonPrimitive?.content

                        val friendIds = friendService.getFriendIds(userId, token)
                        val msg =
                            ConnectionManager.encode(
                                FriendMoodUpdate(
                                    userId = userId,
                                    username = username,
                                    emotion = emotion,
                                    intensity = intensity,
                                    note = note,
                                ),
                            )
                        ConnectionManager.broadcastToUsers(friendIds, msg)
                        log.info("Mood broadcast from $username: $emotion ($intensity) → ${friendIds.size} friends")
                    }

                    "join_room" -> {
                        val payload = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue
                        val emotion = payload["emotion"]?.jsonPrimitive?.content
                        val skinColor = payload["skinColor"]?.jsonPrimitive?.content
                        val clothesColor = payload["clothesColor"]?.jsonPrimitive?.content
                        val hairStyle = payload["hairStyle"]?.jsonPrimitive?.content
                        val hairColor = payload["hairColor"]?.jsonPrimitive?.content

                        // Don't let the owner join their own room as visitor
                        if (roomOwnerId == userId) continue

                        val visitor = RoomVisitor(userId, username, emotion, skinColor, clothesColor, hairStyle, hairColor)
                        RoomManager.joinRoom(roomOwnerId, visitor)

                        // Send current room state to the joining visitor
                        val stateVisitors =
                            RoomManager.getVisitors(roomOwnerId).map { v ->
                                RoomVisitorInfo(v.userId, v.username, v.emotion, v.skinColor, v.clothesColor, v.hairStyle, v.hairColor)
                            }
                        send(ConnectionManager.encode(RoomStateMessage(visitors = stateVisitors)))

                        // Broadcast visitor_entered to owner + existing visitors (excluding the new one)
                        val enteredMsg =
                            ConnectionManager.encode(
                                VisitorEntered(
                                    userId = userId,
                                    username = username,
                                    emotion = emotion,
                                    skinColor = skinColor,
                                    clothesColor = clothesColor,
                                    hairStyle = hairStyle,
                                    hairColor = hairColor,
                                ),
                            )
                        ConnectionManager.sendTo(roomOwnerId, enteredMsg)
                        RoomManager
                            .getVisitors(roomOwnerId)
                            .filter { it.userId != userId }
                            .forEach { ConnectionManager.sendTo(it.userId, enteredMsg) }

                        // Notify room owner via notification service
                        launch {
                            notificationClient.createNotification(
                                userId = roomOwnerId,
                                type = "ROOM_VISIT",
                                fromUserId = userId,
                                fromUsername = username,
                            )
                        }

                        // Update social needs for both visitor and room owner
                        launch {
                            avatarClient.onSocialEvent(userId) // visitor
                            avatarClient.onSocialEvent(roomOwnerId) // owner
                        }

                        log.info("$username joined room of $roomOwnerId")
                    }

                    "leave_room" -> {
                        val payload = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue

                        RoomManager.leaveRoom(roomOwnerId, userId) ?: continue

                        val leftMsg = ConnectionManager.encode(VisitorLeft(userId = userId, username = username))
                        ConnectionManager.sendTo(roomOwnerId, leftMsg)
                        RoomManager.getVisitors(roomOwnerId).forEach { ConnectionManager.sendTo(it.userId, leftMsg) }

                        log.info("$username left room of $roomOwnerId")
                    }

                    "room_reaction" -> {
                        val payload = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue
                        val reaction = payload["reaction"]?.jsonPrimitive?.content ?: continue

                        val msg =
                            ConnectionManager.encode(
                                RoomReactionReceived(fromUserId = userId, fromUsername = username, reaction = reaction),
                            )
                        // Broadcast to owner and all visitors (including sender so they see it too)
                        ConnectionManager.sendTo(roomOwnerId, msg)
                        RoomManager.getVisitors(roomOwnerId).forEach { ConnectionManager.sendTo(it.userId, msg) }
                    }

                    "room_knock" -> {
                        val payload = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue

                        val msg =
                            ConnectionManager.encode(
                                RoomKnocked(fromUserId = userId, fromUsername = username),
                            )
                        ConnectionManager.sendTo(roomOwnerId, msg)
                        log.info("$username knocked on $roomOwnerId's door")
                    }

                    else -> log.debug("Unknown message type '${inbound.type}' from $userId")
                }
            }
        } finally {
            // Leave any rooms the user was visiting and notify remaining occupants
            val leftRooms = RoomManager.leaveAllRooms(userId)
            for ((ownerId, _) in leftRooms) {
                val leftMsg = ConnectionManager.encode(VisitorLeft(userId = userId, username = username))
                ConnectionManager.sendTo(ownerId, leftMsg)
                RoomManager.getVisitors(ownerId).forEach { ConnectionManager.sendTo(it.userId, leftMsg) }
            }

            ConnectionManager.disconnect(userId)
            presenceService.setOffline(userId)

            // Notify friends that this user went offline
            launch {
                val friendIds = friendService.getFriendIds(userId, token)
                val msg = ConnectionManager.encode(FriendOnlineStatus(userId = userId, username = username, online = false))
                ConnectionManager.broadcastToUsers(friendIds, msg)
            }
        }
    }
}
