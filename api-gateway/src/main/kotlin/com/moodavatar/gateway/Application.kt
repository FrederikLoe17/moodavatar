package com.moodavatar.gateway

import com.moodavatar.gateway.plugins.RateLimitPlugin
import com.moodavatar.gateway.plugins.configureRouting
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain
        .main(args)

@Serializable
private data class GatewayErrorResponse(
    val error: String,
    val message: String,
)

fun Application.module() {
    val allowedOrigin =
        environment.config
            .propertyOrNull("gateway.allowedOrigin")
            ?.getString() ?: "http://localhost:5173"

    // ── Content Negotiation ──────────────────────────────────────────────────
    install(ContentNegotiation) {
        json()
    }

    // ── CORS ─────────────────────────────────────────────────────────────────
    install(CORS) {
        allowHost(
            allowedOrigin.removePrefix("http://").removePrefix("https://"),
            schemes = listOf(if (allowedOrigin.startsWith("https")) "https" else "http"),
        )
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        exposeHeader("X-RateLimit-Limit")
        exposeHeader("X-RateLimit-Remaining")
    }

    // ── Rate Limiting ────────────────────────────────────────────────────────
    install(RateLimitPlugin)

    // ── Status Pages (fallback error handling) ───────────────────────────────
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.BadGateway,
                GatewayErrorResponse("BAD_GATEWAY", "Upstream service unavailable"),
            )
        }
    }

    // ── HTTP Client (for proxying) ───────────────────────────────────────────
    val httpClient =
        HttpClient(CIO) {
            expectSuccess = false
            engine {
                requestTimeout = 30_000
            }
        }

    // ── Routing ──────────────────────────────────────────────────────────────
    configureRouting(httpClient)
}
