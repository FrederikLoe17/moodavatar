package com.moodavatar.user.plugins

import com.moodavatar.user.routes.adminRoutes
import com.moodavatar.user.routes.friendRoutes
import com.moodavatar.user.routes.profileRoutes
import com.moodavatar.user.services.AdminService
import com.moodavatar.user.services.EventPublisher
import com.moodavatar.user.services.FriendService
import com.moodavatar.user.services.ProfileService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(eventPublisher: EventPublisher?) {
    val profileService = ProfileService()
    val friendService  = FriendService(eventPublisher)
    val adminService   = AdminService()
    routing {
        profileRoutes(profileService)
        friendRoutes(friendService)
        adminRoutes(adminService)
    }
}
