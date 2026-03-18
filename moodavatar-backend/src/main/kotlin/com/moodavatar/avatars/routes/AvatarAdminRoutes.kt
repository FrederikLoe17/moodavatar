package com.moodavatar.avatars.routes

import com.moodavatar.avatars.dto.ErrorResponse
import com.moodavatar.avatars.services.AvatarService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.avatarAdminRoutes(avatarService: AvatarService) {
    route("/avatars/admin") {
        authenticate("auth-jwt") {
            get("/stats") {
                val role = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("role")?.asString()
                if (role != "ADMIN") {
                    return@get call.respond(
                        HttpStatusCode.Forbidden,
                        ErrorResponse("FORBIDDEN", "Admin access required")
                    )
                }
                call.respond(HttpStatusCode.OK, avatarService.getAdminStats())
            }
        }
    }
}
