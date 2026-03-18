package com.moodavatar.user.routes

import com.moodavatar.user.dto.*
import com.moodavatar.user.services.ProfileService
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
        // GET /users/health
        get("/health") {
            call.respond(HttpStatusCode.OK, MessageResponse("ok"))
        }

        // Interner Endpunkt: Profil anlegen (wird vom Auth Service nach Register aufgerufen)
        post("/internal/profile") {
            val req = call.receive<CreateProfileRequest>()
            runCatching { profileService.createProfile(req) }
                .onSuccess { call.respond(HttpStatusCode.Created, it) }
                .onFailure { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "ERROR", "Profile creation failed")) }
        }

        // GET /users/public/{username} – Öffentliches Profil (kein Auth nötig)
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
            // GET /users/me
            get("/me") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val profile =
                    profileService.getProfile(userId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Profile not found"))
                call.respond(HttpStatusCode.OK, profile)
            }

            // PUT /users/me
            put("/me") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<UpdateProfileRequest>()
                val updated =
                    profileService.updateProfile(userId, req)
                        ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK, updated)
            }

            // GET /users/search?q=username
            get("/search") {
                val query = call.request.queryParameters["q"]?.trim()
                if (query.isNullOrBlank()) {
                    return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("MISSING_QUERY", "Query parameter 'q' is required"))
                }
                call.respond(HttpStatusCode.OK, profileService.searchProfiles(query))
            }

            // GET /users/{id}
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
