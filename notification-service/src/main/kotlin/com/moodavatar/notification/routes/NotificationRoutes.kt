package com.moodavatar.notification.routes

import com.moodavatar.notification.services.NotificationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable data class UnreadCountResponse(val count: Int)
@Serializable data class MessageResponse(val message: String)
@Serializable data class CreateNotificationRequest(
    val userId: String,
    val type: String,
    val fromUserId: String,
    val fromUsername: String,
)

private fun ApplicationCall.userId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

fun Route.notificationRoutes(service: NotificationService) {
    // Internal endpoint — called by other services (no user JWT required)
    post("/notifications/internal/create") {
        val req = call.receive<CreateNotificationRequest>()
        service.create(req.userId, req.type, req.fromUserId, req.fromUsername)
        call.respond(HttpStatusCode.Created, MessageResponse("Created"))
    }

    authenticate("auth-jwt") {
        route("/notifications") {

            // GET /notifications
            get {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(service.listForUser(userId))
            }

            // GET /notifications/unread-count
            get("/unread-count") {
                val userId = call.userId() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(UnreadCountResponse(service.unreadCount(userId)))
            }

            // POST /notifications/read-all
            post("/read-all") {
                val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                service.markAllRead(userId)
                call.respond(MessageResponse("All notifications marked as read"))
            }

            // POST /notifications/{id}/read
            post("/{id}/read") {
                val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val id     = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val ok = service.markRead(id, userId)
                if (ok) call.respond(MessageResponse("Marked as read"))
                else    call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
