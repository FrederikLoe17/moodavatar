package com.moodavatar.avatars.routes

import com.moodavatar.avatars.dto.ErrorResponse
import com.moodavatar.avatars.dto.MessageResponse
import com.moodavatar.avatars.dto.SetMoodRequest
import com.moodavatar.avatars.dto.UpdateConfigRequest
import com.moodavatar.avatars.models.Emotion
import com.moodavatar.avatars.services.AvatarService
import com.moodavatar.avatars.services.NeedsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.avatarRoutes(
    avatarService: AvatarService,
    needsService: NeedsService,
) {
    route("/avatars") {
        // GET /avatars/health
        get("/health") {
            call.respond(HttpStatusCode.OK, MessageResponse("ok"))
        }

        // GET /avatars/public/{userId} – no auth required
        get("/public/{userId}") {
            val userId = call.parameters["userId"]
                ?.let { runCatching { UUID.fromString(it); it }.getOrNull() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid user ID"))
            val avatar = avatarService.getAvatar(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Avatar not found"))
            call.respond(HttpStatusCode.OK, avatar)
        }
    }

    authenticate("auth-jwt") {
        route("/avatars") {
            get("/me") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val avatar = avatarService.getAvatar(userId)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("NOT_FOUND", "Avatar not found. Set a mood first."),
                    )
                call.respond(HttpStatusCode.OK, avatar)
            }

            put("/me/config") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val req    = call.receive<UpdateConfigRequest>()
                val avatar = avatarService.updateConfig(userId, req)
                call.respond(HttpStatusCode.OK, avatar)
            }

            put("/me/mood") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val req    = call.receive<SetMoodRequest>()

                if (!isValidEmotion(req.emotion)) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_EMOTION", "Valid emotions: ${Emotion.entries.joinToString()}")
                    )
                }
                if (req.intensity !in 1..10) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_INTENSITY", "Intensity must be between 1 and 10")
                    )
                }

                val avatar = avatarService.setMood(userId, req.emotion, req.intensity, req.note)
                needsService.onMoodCheckin(userId)
                call.respond(HttpStatusCode.OK, avatar)
            }

            get("/me/history") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val limit  = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 20
                val history = avatarService.getMoodHistory(userId, limit)
                call.respond(HttpStatusCode.OK, history)
            }

            get("/me/history/calendar") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val days   = call.request.queryParameters["days"]?.toIntOrNull()?.coerceIn(7, 365) ?: 90
                call.respond(HttpStatusCode.OK, avatarService.getCalendarData(userId, days))
            }

            get("/me/insights") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(HttpStatusCode.OK, avatarService.getInsights(userId))
            }

            get("/me/needs") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(HttpStatusCode.OK, needsService.getNeeds(userId))
            }

            get("/{userId}") {
                val targetId = call.parameters["userId"]
                    ?.let { runCatching { UUID.fromString(it); it }.getOrNull() }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid user ID"))
                val avatar = avatarService.getAvatar(targetId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Avatar not found"))
                call.respond(HttpStatusCode.OK, avatar)
            }
        }
    }
}

private fun ApplicationCall.userId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

private fun isValidEmotion(value: String) = runCatching { Emotion.valueOf(value.uppercase()) }.isSuccess
