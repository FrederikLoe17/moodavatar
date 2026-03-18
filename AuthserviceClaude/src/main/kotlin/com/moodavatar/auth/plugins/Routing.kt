package com.moodavatar.auth.plugins

import com.moodavatar.auth.routes.adminRoutes
import com.moodavatar.auth.routes.authRoutes
import com.moodavatar.auth.services.AdminService
import com.moodavatar.auth.services.AuthService
import com.moodavatar.auth.services.EmailService
import com.moodavatar.auth.services.UserServiceClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    val cfg          = environment.config
    val authService  = AuthService(cfg)
    val adminService = AdminService()
    val emailService = EmailService(cfg)

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    val userServiceUrl    = cfg.propertyOrNull("services.userServiceUrl")?.getString() ?: "http://user-service:8082"
    val userServiceClient = UserServiceClient(httpClient, userServiceUrl)

    environment.monitor.subscribe(ApplicationStopped) { httpClient.close() }

    routing {
        authRoutes(authService, emailService, userServiceClient)
        adminRoutes(adminService)
    }
}