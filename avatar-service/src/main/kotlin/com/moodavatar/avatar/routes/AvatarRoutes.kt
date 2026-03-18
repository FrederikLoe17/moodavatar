package com.moodavatar.avatar.routes

import com.moodavatar.avatar.dto.*
import com.moodavatar.avatar.models.Emotion
import com.moodavatar.avatar.services.AvatarService
import com.moodavatar.avatar.services.NeedsService
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
    // GET /avatars/public/{userId} – Avatar öffentlich abrufen (kein Auth nötig)
    route("/avatars") {
        get("/public/{userId}") {
            val userId =
                call.parameters["userId"]
                    ?.let {
                        runCatching {
                            UUID.fromString(it)
                            it
                        }.getOrNull()
                    }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid user ID"))
            val avatar =
                avatarService.getAvatar(userId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Avatar not found"))
            call.respond(HttpStatusCode.OK, avatar)
        }
    }

    authenticate("auth-jwt") {
        route("/avatars") {
            // GET /avatars/me – Eigenen Avatar abrufen
            get("/me") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val avatar =
                    avatarService.getAvatar(userId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse("NOT_FOUND", "Avatar not found. Set a mood first."),
                        )
                call.respond(HttpStatusCode.OK, avatar)
            }

            // PUT /avatars/me/config – Avatar-Personalisierung speichern
            put("/me/config") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<UpdateConfigRequest>()
                val avatar = avatarService.updateConfig(userId, req)
                call.respond(HttpStatusCode.OK, avatar)
            }

            // PUT /avatars/me/mood – Stimmung setzen
            put("/me/mood") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<SetMoodRequest>()

                // Validierung
                if (!isValidEmotion(req.emotion)) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_EMOTION", "Valid emotions: ${Emotion.entries.joinToString()}"),
                    )
                }
                if (req.intensity !in 1..10) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_INTENSITY", "Intensity must be between 1 and 10"),
                    )
                }

                val avatar = avatarService.setMood(userId, req.emotion, req.intensity, req.note)
                needsService.onMoodCheckin(userId)
                call.respond(HttpStatusCode.OK, avatar)
            }

            // GET /avatars/me/history – Eigene Mood-Historie
            get("/me/history") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val limit =
                    call.request.queryParameters["limit"]
                        ?.toIntOrNull()
                        ?.coerceIn(1, 50) ?: 20
                val history = avatarService.getMoodHistory(userId, limit)
                call.respond(HttpStatusCode.OK, history)
            }

            // GET /avatars/me/needs
            get("/me/needs") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(HttpStatusCode.OK, needsService.getNeeds(userId))
            }

            // GET /avatars/{userId} – Avatar eines anderen Users abrufen
            get("/{userId}") {
                val targetId =
                    call.parameters["userId"]
                        ?.let {
                            runCatching {
                                UUID.fromString(it)
                                it
                            }.getOrNull()
                        }
                        ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid user ID"))

                val avatar =
                    avatarService.getAvatar(targetId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse("NOT_FOUND", "Avatar not found"),
                        )
                call.respond(HttpStatusCode.OK, avatar)
            }
        }
    }
}

// POST /avatars/internal/needs/social?userId=X  (called by realtime-service)
fun Route.avatarInternalRoutes(needsService: NeedsService) {
    post("/avatars/internal/needs/social") {
        val userId =
            call.request.queryParameters["userId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest)
        needsService.onSocialEvent(userId)
        call.respond(HttpStatusCode.OK, mapOf("ok" to true))
    }
}

private fun ApplicationCall.userId(): String? = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

private fun isValidEmotion(value: String) = runCatching { Emotion.valueOf(value.uppercase()) }.isSuccess
