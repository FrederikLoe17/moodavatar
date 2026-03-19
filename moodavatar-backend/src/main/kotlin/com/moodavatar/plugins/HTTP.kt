package com.moodavatar.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHTTP() {
    val cfg = environment.config
    val allowedOrigin = cfg.propertyOrNull("app.allowedOrigin")?.getString() ?: "*"

    install(CORS) {
        if (allowedOrigin == "*") {
            anyHost()
        } else {
            allowHost(
                allowedOrigin
                    .removePrefix("https://")
                    .removePrefix("http://"),
                schemes = listOf("http", "https"),
            )
        }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowCredentials = true
    }
}
