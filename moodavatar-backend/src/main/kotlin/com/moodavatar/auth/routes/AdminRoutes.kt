package com.moodavatar.auth.routes

import com.moodavatar.auth.dto.ErrorResponse
import com.moodavatar.auth.dto.UpdateRoleRequest
import com.moodavatar.auth.models.Role
import com.moodavatar.auth.services.AdminService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

private val FORBIDDEN = ErrorResponse("FORBIDDEN", "Admin access required")

private fun ApplicationCall.isAdmin(): Boolean =
    principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString() == "ADMIN"

fun Route.adminRoutes(adminService: AdminService) {
    route("/auth/admin") {
        authenticate("auth-jwt") {
            get("/stats") {
                if (!call.isAdmin()) return@get call.respond(HttpStatusCode.Forbidden, FORBIDDEN)
                call.respond(HttpStatusCode.OK, adminService.getStats())
            }

            get("/users") {
                if (!call.isAdmin()) return@get call.respond(HttpStatusCode.Forbidden, FORBIDDEN)
                val page     = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
                val search   = call.request.queryParameters["search"]
                call.respond(HttpStatusCode.OK, adminService.listUsers(page, pageSize, search))
            }

            put("/users/{id}/role") {
                if (!call.isAdmin()) return@put call.respond(HttpStatusCode.Forbidden, FORBIDDEN)
                val userId = call.parameters["id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid user ID"))
                val req  = call.receive<UpdateRoleRequest>()
                val role = runCatching { Role.valueOf(req.role.uppercase()) }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ROLE", "Valid roles: USER, ADMIN"))
                val updated = adminService.updateRole(userId, role)
                    ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "User not found"))
                call.respond(HttpStatusCode.OK, updated)
            }
        }
    }
}
