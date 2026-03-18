package com.moodavatar.realtime.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.moodavatar.avatars.services.NeedsService
import com.moodavatar.notifications.services.NotificationService
import com.moodavatar.realtime.models.*
import com.moodavatar.realtime.services.ConnectedUser
import com.moodavatar.realtime.services.ConnectionManager
import com.moodavatar.realtime.services.RoomManager
import com.moodavatar.realtime.services.RoomVisitor
import com.moodavatar.users.services.FriendService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

private val log  = LoggerFactory.getLogger("WebSocketRoutes")
private val json = Json { ignoreUnknownKeys = true }

fun Route.webSocketRoutes(
    jwtSecret: String,
    friendService: FriendService,
    notificationService: NotificationService,
    needsService: NeedsService,
    connectionManager: ConnectionManager,
    roomManager: RoomManager,
) {
    val algorithm = Algorithm.HMAC256(jwtSecret)
    val verifier  = JWT.require(algorithm)
        .withIssuer("moodavatar")
        .withAudience("moodavatar-users")
        .build()

    webSocket("/ws") {
        val token = call.request.queryParameters["token"]
        if (token == null) {
            send(json.encodeToString(ErrorMessage.serializer(), ErrorMessage(message = "Missing token")))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
            return@webSocket
        }

        val decodedJwt = try {
            verifier.verify(token)
        } catch (e: Exception) {
            send(json.encodeToString(ErrorMessage.serializer(), ErrorMessage(message = "Invalid token")))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
            return@webSocket
        }

        val userId   = decodedJwt.getClaim("userId").asString()
        val username = decodedJwt.getClaim("username").asString() ?: userId
        if (userId == null) {
            send(connectionManager.encode(ErrorMessage(message = "userId claim missing")))
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "userId claim missing"))
            return@webSocket
        }

        val user = ConnectedUser(userId, username, this)
        connectionManager.connect(user)

        // Notify friends online
        launch {
            val friendIds = friendService.getFriendIds(userId)
            val msg       = connectionManager.encode(FriendOnlineStatus(userId = userId, username = username, online = true))
            connectionManager.broadcastToUsers(friendIds, msg)
        }

        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()

                val inbound = try {
                    json.decodeFromString<InboundMessage>(text)
                } catch (e: Exception) {
                    log.warn("Unparseable message from $userId: $text")
                    continue
                }

                when (inbound.type) {
                    "ping" -> {
                        send(connectionManager.encode(PongMessage()))
                    }

                    "mood_update" -> {
                        val payload   = inbound.payload?.jsonObject ?: continue
                        val emotion   = payload["emotion"]?.jsonPrimitive?.content ?: continue
                        val intensity = payload["intensity"]?.jsonPrimitive?.content?.toIntOrNull() ?: 5
                        val note      = payload["note"]?.jsonPrimitive?.content

                        val friendIds = friendService.getFriendIds(userId)
                        val msg = connectionManager.encode(
                            FriendMoodUpdate(
                                userId    = userId,
                                username  = username,
                                emotion   = emotion,
                                intensity = intensity,
                                note      = note,
                            )
                        )
                        connectionManager.broadcastToUsers(friendIds, msg)
                        log.info("Mood broadcast from $username: $emotion ($intensity) → ${friendIds.size} friends")
                    }

                    "join_room" -> {
                        val payload      = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId  = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue
                        val emotion      = payload["emotion"]?.jsonPrimitive?.content
                        val skinColor    = payload["skinColor"]?.jsonPrimitive?.content
                        val clothesColor = payload["clothesColor"]?.jsonPrimitive?.content
                        val hairStyle    = payload["hairStyle"]?.jsonPrimitive?.content
                        val hairColor    = payload["hairColor"]?.jsonPrimitive?.content

                        if (roomOwnerId == userId) continue

                        val visitor = RoomVisitor(userId, username, emotion, skinColor, clothesColor, hairStyle, hairColor)
                        roomManager.joinRoom(roomOwnerId, visitor)

                        // Send current room state to joining visitor
                        val stateVisitors = roomManager.getVisitors(roomOwnerId).map { v ->
                            RoomVisitorInfo(v.userId, v.username, v.emotion, v.skinColor, v.clothesColor, v.hairStyle, v.hairColor)
                        }
                        send(connectionManager.encode(RoomStateMessage(visitors = stateVisitors)))

                        // Broadcast visitor_entered to owner + existing visitors
                        val enteredMsg = connectionManager.encode(
                            VisitorEntered(
                                userId       = userId,
                                username     = username,
                                emotion      = emotion,
                                skinColor    = skinColor,
                                clothesColor = clothesColor,
                                hairStyle    = hairStyle,
                                hairColor    = hairColor,
                            )
                        )
                        connectionManager.sendTo(roomOwnerId, enteredMsg)
                        roomManager.getVisitors(roomOwnerId)
                            .filter { it.userId != userId }
                            .forEach { connectionManager.sendTo(it.userId, enteredMsg) }

                        // Notification + social needs — direct service calls
                        launch {
                            notificationService.create(
                                userId       = roomOwnerId,
                                type         = "ROOM_VISIT",
                                fromUserId   = userId,
                                fromUsername = username,
                            )
                        }
                        launch {
                            needsService.onSocialEvent(userId)
                            needsService.onSocialEvent(roomOwnerId)
                        }

                        log.info("$username joined room of $roomOwnerId")
                    }

                    "leave_room" -> {
                        val payload     = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue

                        roomManager.leaveRoom(roomOwnerId, userId) ?: continue

                        val leftMsg = connectionManager.encode(VisitorLeft(userId = userId, username = username))
                        connectionManager.sendTo(roomOwnerId, leftMsg)
                        roomManager.getVisitors(roomOwnerId).forEach { connectionManager.sendTo(it.userId, leftMsg) }

                        log.info("$username left room of $roomOwnerId")
                    }

                    "room_reaction" -> {
                        val payload     = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue
                        val reaction    = payload["reaction"]?.jsonPrimitive?.content ?: continue

                        val msg = connectionManager.encode(
                            RoomReactionReceived(fromUserId = userId, fromUsername = username, reaction = reaction)
                        )
                        connectionManager.sendTo(roomOwnerId, msg)
                        roomManager.getVisitors(roomOwnerId).forEach { connectionManager.sendTo(it.userId, msg) }
                    }

                    "room_knock" -> {
                        val payload     = inbound.payload?.jsonObject ?: continue
                        val roomOwnerId = payload["roomOwnerId"]?.jsonPrimitive?.content ?: continue

                        val msg = connectionManager.encode(RoomKnocked(fromUserId = userId, fromUsername = username))
                        connectionManager.sendTo(roomOwnerId, msg)
                        log.info("$username knocked on $roomOwnerId's door")
                    }

                    else -> log.debug("Unknown message type '${inbound.type}' from $userId")
                }
            }
        } finally {
            val leftRooms = roomManager.leaveAllRooms(userId)
            for ((ownerId, _) in leftRooms) {
                val leftMsg = connectionManager.encode(VisitorLeft(userId = userId, username = username))
                connectionManager.sendTo(ownerId, leftMsg)
                roomManager.getVisitors(ownerId).forEach { connectionManager.sendTo(it.userId, leftMsg) }
            }

            connectionManager.disconnect(userId)

            launch {
                val friendIds = friendService.getFriendIds(userId)
                val msg = connectionManager.encode(FriendOnlineStatus(userId = userId, username = username, online = false))
                connectionManager.broadcastToUsers(friendIds, msg)
            }
        }
    }
}
