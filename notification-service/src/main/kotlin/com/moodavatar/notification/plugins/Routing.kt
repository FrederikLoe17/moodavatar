package com.moodavatar.notification.plugins

import com.moodavatar.notification.routes.notificationRoutes
import com.moodavatar.notification.services.NotificationService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(service: NotificationService) {
    routing {
        get("/health") { call.respondText("OK") }
        notificationRoutes(service)
    }
}
