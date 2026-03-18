package com.moodavatar.realtime

import com.moodavatar.realtime.plugins.configureRouting
import com.moodavatar.realtime.services.AvatarClient
import com.moodavatar.realtime.services.FriendService
import com.moodavatar.realtime.services.NotificationClient
import com.moodavatar.realtime.services.PresenceService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.time.Duration

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val jwtSecret              = environment.config.property("jwt.secret").getString()
    val userServiceUrl         = environment.config.property("services.userService").getString()
    val notificationServiceUrl = environment.config.property("services.notificationService").getString()
    val avatarServiceUrl       = environment.config.property("services.avatarService").getString()
    val redisHost              = environment.config.property("redis.host").getString()
    val redisPort              = environment.config.property("redis.port").getString().toInt()

    // HTTP client for outbound calls (to user-service)
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // WebSocket support
    install(WebSockets) {
        pingPeriod     = Duration.ofSeconds(30)
        timeout        = Duration.ofSeconds(60)
        maxFrameSize   = Long.MAX_VALUE
        masking        = false
    }

    install(CallLogging) { level = Level.INFO }

    val presenceService      = PresenceService(redisHost, redisPort)
    val friendService        = FriendService(httpClient, userServiceUrl)
    val notificationClient   = NotificationClient(httpClient, notificationServiceUrl)
    val avatarClient         = AvatarClient(httpClient, avatarServiceUrl)

    environment.monitor.subscribe(ApplicationStopped) {
        presenceService.close()
        httpClient.close()
    }

    configureRouting(jwtSecret, friendService, presenceService, notificationClient, avatarClient)
}
