package com.moodavatar.user.routes

import com.moodavatar.user.dto.ErrorResponse
import com.moodavatar.user.services.AdminService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes(adminService: AdminService) {
    route("/users/admin") {
        authenticate("auth-jwt") {

            // GET /users/admin/stats
            get("/stats") {
                val role = call.principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
                if (role != "ADMIN") return@get call.respond(
                    HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", "Admin access required")
                )
                call.respond(HttpStatusCode.OK, adminService.getStats())
            }
        }
    }
}
