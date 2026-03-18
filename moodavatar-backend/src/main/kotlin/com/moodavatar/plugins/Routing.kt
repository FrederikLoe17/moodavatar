package com.moodavatar.plugins

import com.mongodb.client.MongoDatabase
import com.moodavatar.auth.routes.adminRoutes
import com.moodavatar.auth.routes.authRoutes
import com.moodavatar.auth.services.AdminService
import com.moodavatar.auth.services.AuthService
import com.moodavatar.auth.services.EmailService
import com.moodavatar.avatars.routes.avatarAdminRoutes
import com.moodavatar.avatars.routes.avatarRoutes
import com.moodavatar.avatars.services.AvatarService
import com.moodavatar.avatars.services.NeedsService
import com.moodavatar.notifications.routes.notificationRoutes
import com.moodavatar.notifications.services.NotificationService
import com.moodavatar.realtime.routes.webSocketRoutes
import com.moodavatar.realtime.services.ConnectionManager
import com.moodavatar.realtime.services.RoomManager
import com.moodavatar.users.routes.friendRoutes
import com.moodavatar.users.routes.profileRoutes
import com.moodavatar.users.routes.userAdminRoutes
import com.moodavatar.users.services.FriendService
import com.moodavatar.users.services.ProfileService
import com.moodavatar.users.services.UserAdminService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String, val service: String)

fun Application.configureRouting(mongoDb: MongoDatabase) {
    val cfg = environment.config
    val jwtSecret = cfg.property("jwt.secret").getString()

    // ── Service instances ────────────────────────────────────────────────────
    val authService         = AuthService(cfg)
    val authAdminService    = AdminService()
    val emailService        = EmailService(cfg)
    val profileService      = ProfileService()
    val friendService       = FriendService()
    val userAdminService    = UserAdminService()
    val avatarService       = AvatarService(mongoDb)
    val needsService        = NeedsService(mongoDb)
    val notificationService = NotificationService()
    val connectionManager   = ConnectionManager
    val roomManager         = RoomManager

    routing {
        // ── Health ──────────────────────────────────────────────────────────
        get("/health") {
            call.respond(HealthResponse(status = "ok", service = "moodavatar-backend"))
        }
        get("/system/health") {
            call.respond(HealthResponse(status = "ok", service = "moodavatar-backend"))
        }

        // ── Auth ────────────────────────────────────────────────────────────
        authRoutes(authService, emailService, profileService)
        adminRoutes(authAdminService)

        // ── Users / Profiles ────────────────────────────────────────────────
        profileRoutes(profileService)
        friendRoutes(friendService, notificationService)
        userAdminRoutes(userAdminService)

        // ── Avatars ─────────────────────────────────────────────────────────
        avatarRoutes(avatarService, needsService)
        avatarAdminRoutes(avatarService)

        // ── Notifications ────────────────────────────────────────────────────
        notificationRoutes(notificationService)

        // ── WebSocket ────────────────────────────────────────────────────────
        webSocketRoutes(
            jwtSecret = jwtSecret,
            friendService = friendService,
            notificationService = notificationService,
            needsService = needsService,
            connectionManager = connectionManager,
            roomManager = roomManager,
        )
    }
}
