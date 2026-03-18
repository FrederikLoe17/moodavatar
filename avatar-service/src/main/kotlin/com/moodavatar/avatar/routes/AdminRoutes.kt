package com.moodavatar.avatar.routes

import com.moodavatar.avatar.dto.ErrorResponse
import com.moodavatar.avatar.dto.MessageResponse
import com.moodavatar.avatar.services.AvatarService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.avatarAdminRoutes(avatarService: AvatarService) {
    // GET /avatars/health (public)
    get("/avatars/health") {
        call.respond(HttpStatusCode.OK, MessageResponse("ok"))
    }

    route("/avatars/admin") {
        authenticate("auth-jwt") {
            // GET /avatars/admin/stats
            get("/stats") {
                val role =
                    call
                        .principal<JWTPrincipal>()
                        ?.payload
                        ?.getClaim("role")
                        ?.asString()
                if (role != "ADMIN") {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("FORBIDDEN", "Admin access required"),
                    )
                }
                val stats = avatarService.getAdminStats()
                call.respond(HttpStatusCode.OK, stats)
            }
        }
    }
}
