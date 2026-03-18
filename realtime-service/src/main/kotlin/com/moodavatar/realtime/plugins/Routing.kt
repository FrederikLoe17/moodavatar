package com.moodavatar.realtime.plugins

import com.moodavatar.realtime.routes.webSocketRoutes
import com.moodavatar.realtime.services.AvatarClient
import com.moodavatar.realtime.services.FriendService
import com.moodavatar.realtime.services.NotificationClient
import com.moodavatar.realtime.services.PresenceService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    jwtSecret: String,
    friendService: FriendService,
    presenceService: PresenceService,
    notificationClient: NotificationClient,
    avatarClient: AvatarClient,
) {
    routing {
        get("/health") {
            call.respondText("OK")
        }

        webSocketRoutes(jwtSecret, friendService, presenceService, notificationClient, avatarClient)
    }
}
