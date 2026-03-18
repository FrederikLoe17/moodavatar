package com.moodavatar.notifications.routes

import com.moodavatar.notifications.dto.MessageResponse
import com.moodavatar.notifications.dto.UnreadCountResponse
import com.moodavatar.notifications.services.NotificationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun ApplicationCall.userId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

fun Route.notificationRoutes(service: NotificationService) {
    authenticate("auth-jwt") {
        route("/notifications") {
            get {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(service.listForUser(userId))
            }

            get("/unread-count") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(UnreadCountResponse(service.unreadCount(userId)))
            }

            post("/read-all") {
                val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                service.markAllRead(userId)
                call.respond(MessageResponse("All notifications marked as read"))
            }

            post("/{id}/read") {
                val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val id     = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val ok     = service.markRead(id, userId)
                if (ok) {
                    call.respond(MessageResponse("Marked as read"))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
