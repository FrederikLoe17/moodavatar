package com.moodavatar.users.routes

import com.moodavatar.notifications.services.NotificationService
import com.moodavatar.users.dto.ErrorResponse
import com.moodavatar.users.dto.FriendRequestAction
import com.moodavatar.users.dto.FriendRequestResponse
import com.moodavatar.users.dto.MessageResponse
import com.moodavatar.users.dto.ProfileResponse
import com.moodavatar.users.services.FriendService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

private fun ApplicationCall.extractUserId(): UUID? =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("userId")
        ?.asString()
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

private suspend fun ApplicationCall.unauthorized() =
    respond<ErrorResponse>(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Missing or invalid token"))

private suspend fun ApplicationCall.badRequest(msg: String) =
    respond<ErrorResponse>(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", msg))

fun Route.friendRoutes(
    friendService: FriendService,
    notificationService: NotificationService,
) {
    authenticate("auth-jwt") {
        route("/friends") {
            get {
                val userId =
                    call.extractUserId() ?: run {
                        call.unauthorized()
                        return@get
                    }
                call.respond<List<ProfileResponse>>(HttpStatusCode.OK, friendService.getFriends(userId))
            }

            get("/requests") {
                val userId =
                    call.extractUserId() ?: run {
                        call.unauthorized()
                        return@get
                    }
                call.respond<List<FriendRequestResponse>>(HttpStatusCode.OK, friendService.getPendingRequests(userId))
            }

            post("/requests/{receiverId}") {
                val userId =
                    call.extractUserId() ?: run {
                        call.unauthorized()
                        return@post
                    }
                val receiverId =
                    call.parameters["receiverId"]
                        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                        ?: run {
                            call.badRequest("Invalid receiver ID")
                            return@post
                        }

                try {
                    val result = friendService.sendRequest(userId, receiverId, notificationService)
                    call.respond<FriendRequestResponse>(HttpStatusCode.Created, result)
                } catch (e: Exception) {
                    val msg = e.message ?: "ERROR"
                    val status = if (msg == "USER_NOT_FOUND") HttpStatusCode.NotFound else HttpStatusCode.BadRequest
                    call.respond<ErrorResponse>(status, ErrorResponse(msg, "Friend request failed"))
                }
            }

            patch("/requests/{requestId}") {
                val userId =
                    call.extractUserId() ?: run {
                        call.unauthorized()
                        return@patch
                    }
                val requestId =
                    call.parameters["requestId"]
                        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                        ?: run {
                            call.badRequest("Invalid request ID")
                            return@patch
                        }

                val body = call.receive<FriendRequestAction>()
                try {
                    val result = friendService.respondToRequest(requestId, userId, body.action, notificationService)
                    call.respond<FriendRequestResponse>(HttpStatusCode.OK, result)
                } catch (e: Exception) {
                    call.respond<ErrorResponse>(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "ERROR", "Action failed"))
                }
            }

            delete("/{friendId}") {
                val userId =
                    call.extractUserId() ?: run {
                        call.unauthorized()
                        return@delete
                    }
                val friendId =
                    call.parameters["friendId"]
                        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                        ?: run {
                            call.badRequest("Invalid friend ID")
                            return@delete
                        }

                val removed = friendService.removeFriend(userId, friendId)
                if (removed) {
                    call.respond<MessageResponse>(HttpStatusCode.OK, MessageResponse("Friend removed"))
                } else {
                    call.respond<ErrorResponse>(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Friendship not found"))
                }
            }
        }
    }
}
