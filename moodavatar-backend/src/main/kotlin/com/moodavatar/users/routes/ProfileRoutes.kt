package com.moodavatar.users.routes

import com.moodavatar.users.dto.ErrorResponse
import com.moodavatar.users.dto.MessageResponse
import com.moodavatar.users.dto.UpdateProfileRequest
import com.moodavatar.users.services.ProfileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.profileRoutes(profileService: ProfileService) {
    route("/users") {
        get("/health") {
            call.respond(HttpStatusCode.OK, MessageResponse("ok"))
        }

        get("/public/{username}") {
            val username =
                call.parameters["username"]?.trim()
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
            val profile =
                profileService.getProfileByUsername(username)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "User not found"))
            call.respond(HttpStatusCode.OK, profile)
        }

        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val profile =
                    profileService.getProfile(userId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Profile not found"))
                call.respond(HttpStatusCode.OK, profile)
            }

            put("/me") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<UpdateProfileRequest>()
                val updated =
                    profileService.updateProfile(userId, req)
                        ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK, updated)
            }

            get("/search") {
                val query = call.request.queryParameters["q"]?.trim()
                if (query.isNullOrBlank()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("MISSING_QUERY", "Query parameter 'q' is required"))
                }
                call.respond(HttpStatusCode.OK, profileService.searchProfiles(query))
            }

            get("/{id}") {
                val id =
                    call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                val profile =
                    profileService.getProfile(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK, profile)
            }
        }
    }
}

fun ApplicationCall.userId(): UUID? =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("userId")
        ?.asString()
        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
