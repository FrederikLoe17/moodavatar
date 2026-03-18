package com.moodavatar.users.routes

import com.moodavatar.users.dto.ErrorResponse
import com.moodavatar.users.services.UserAdminService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userAdminRoutes(adminService: UserAdminService) {
    route("/users/admin") {
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
                call.respond(HttpStatusCode.OK, adminService.getStats())
            }
        }
    }
}
