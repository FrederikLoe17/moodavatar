package com.moodavatar.avatar.plugins

import com.moodavatar.avatar.routes.avatarAdminRoutes
import com.moodavatar.avatar.routes.avatarInternalRoutes
import com.moodavatar.avatar.routes.avatarRoutes
import com.moodavatar.avatar.services.AvatarService
import com.moodavatar.avatar.services.NeedsService
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(db: MongoDatabase) {
    val avatarService = AvatarService(db)
    val needsService  = NeedsService(db)
    routing {
        avatarRoutes(avatarService, needsService)
        avatarAdminRoutes(avatarService)
        avatarInternalRoutes(needsService)
    }
}
