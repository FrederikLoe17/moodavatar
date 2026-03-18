package com.moodavatar.gateway.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.Base64

@Serializable
private data class GatewayError(
    val error: String,
    val message: String,
)

@Serializable
private data class ServiceHealth(
    val name: String,
    val ok: Boolean,
    val responseMs: Long,
)

@Serializable
private data class SystemHealthResponse(
    val services: List<ServiceHealth>,
    val rabbitmq: ServiceHealth,
    val timestamp: String,
)

private val PUBLIC_ROUTES =
    setOf(
        Pair(HttpMethod.Post, "/auth/login"),
        Pair(HttpMethod.Post, "/auth/register"),
        Pair(HttpMethod.Post, "/auth/refresh"),
        Pair(HttpMethod.Post, "/auth/forgot-password"),
        Pair(HttpMethod.Post, "/auth/reset-password"),
        Pair(HttpMethod.Post, "/auth/verify-email"),
        Pair(HttpMethod.Post, "/auth/resend-verification"),
    )

private val PUBLIC_GET_PREFIXES =
    listOf(
        "/users/public/",
        "/avatars/public/",
        "/auth/health",
        "/users/health",
        "/avatars/health",
        "/notifications/health",
    )

fun Application.configureRouting(httpClient: HttpClient) {
    val cfg = environment.config
    val jwtSecret = cfg.property("jwt.secret").getString()
    val issuer = cfg.property("jwt.issuer").getString()
    val audience = cfg.property("jwt.audience").getString()
    val authUrl = cfg.property("services.authUrl").getString().trimEnd('/')
    val userUrl = cfg.property("services.userUrl").getString().trimEnd('/')
    val avatarUrl = cfg.property("services.avatarUrl").getString().trimEnd('/')
    val notificationUrl = cfg.property("services.notificationUrl").getString().trimEnd('/')
    val realtimeUrl = "http://realtime-service:8084"

    val verifier =
        JWT
            .require(Algorithm.HMAC256(jwtSecret))
            .withIssuer(issuer)
            .withAudience(audience)
            .build()

    suspend fun checkService(
        name: String,
        url: String,
    ): ServiceHealth {
        val start = System.currentTimeMillis()
        return try {
            val status = httpClient.get(url).status
            ServiceHealth(name, status.isSuccess(), System.currentTimeMillis() - start)
        } catch (_: Exception) {
            ServiceHealth(name, false, System.currentTimeMillis() - start)
        }
    }

    routing {
        // ── System Health (public, no proxy) ──────────────────────────────────
        get("/system/health") {
            val results =
                coroutineScope {
                    listOf(
                        async { checkService("api-gateway", "$authUrl/auth/health") },
                        async { checkService("auth-service", "$authUrl/auth/health") },
                        async { checkService("user-service", "$userUrl/users/health") },
                        async { checkService("avatar-service", "$avatarUrl/avatars/health") },
                        async { checkService("notification-service", "$notificationUrl/health") },
                        async { checkService("realtime-service", "$realtimeUrl/health") },
                    ).map { it.await() }
                }

            val rabbitMQCreds = Base64.getEncoder().encodeToString("guest:guest".toByteArray())
            val rmq =
                try {
                    val start = System.currentTimeMillis()
                    val status =
                        httpClient
                            .get("http://rabbitmq:15672/api/overview") {
                                header("Authorization", "Basic $rabbitMQCreds")
                            }.status
                    ServiceHealth("rabbitmq", status.isSuccess(), System.currentTimeMillis() - start)
                } catch (_: Exception) {
                    ServiceHealth("rabbitmq", false, 0)
                }

            // Gateway itself is always "ok" (we're responding)
            val services = results.toMutableList()
            services[0] = ServiceHealth("api-gateway", true, 0)

            call.respond(
                HttpStatusCode.OK,
                SystemHealthResponse(
                    services = services,
                    rabbitmq = rmq,
                    timestamp = Instant.now().toString(),
                ),
            )
        }

        route("{...}") {
            handle {
                val method = call.request.httpMethod
                val path = call.request.uri
                val requestPath = call.request.path()

                // ── JWT check ────────────────────────────────────────────────
                val isPublic =
                    PUBLIC_ROUTES.any { (m, p) ->
                        m == method && requestPath.equals(p, ignoreCase = true)
                    } ||
                        (
                            method == HttpMethod.Get &&
                                PUBLIC_GET_PREFIXES.any {
                                    requestPath.startsWith(it, ignoreCase = true)
                                }
                        )

                var userId: String? = null
                var userRole: String? = null
                if (!isPublic) {
                    val token =
                        call.request
                            .authorization()
                            ?.removePrefix("Bearer ")
                            ?.trim()
                    if (token == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            GatewayError("MISSING_TOKEN", "Authorization header required"),
                        )
                        return@handle
                    }
                    try {
                        val decoded = verifier.verify(token)
                        userId = decoded.subject
                        userRole = decoded.getClaim("role")?.asString()
                    } catch (e: JWTVerificationException) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            GatewayError("INVALID_TOKEN", e.message ?: "Token invalid"),
                        )
                        return@handle
                    }

                    // Admin-Route-Schutz
                    val isAdminRoute =
                        requestPath.startsWith("/auth/admin", ignoreCase = true) ||
                            requestPath.startsWith("/users/admin", ignoreCase = true) ||
                            requestPath.startsWith("/avatars/admin", ignoreCase = true)
                    if (isAdminRoute && userRole != "ADMIN") {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            GatewayError("FORBIDDEN", "Admin access required"),
                        )
                        return@handle
                    }
                }

                // ── Target resolution ────────────────────────────────────────
                val targetBase =
                    when {
                        requestPath.startsWith("/auth") -> authUrl
                        requestPath.startsWith("/users") -> userUrl
                        requestPath.startsWith("/friends") -> userUrl
                        requestPath.startsWith("/avatars") -> avatarUrl
                        requestPath.startsWith("/notifications") -> notificationUrl
                        else -> {
                            call.respond(
                                HttpStatusCode.NotFound,
                                GatewayError("NOT_FOUND", "No route for $requestPath"),
                            )
                            return@handle
                        }
                    }

                val targetUrl = "$targetBase$path"

                // ── Read request body ────────────────────────────────────────
                val body =
                    try {
                        call.receive<ByteArray>()
                    } catch (_: Exception) {
                        ByteArray(0)
                    }

                // ── Proxy the request ────────────────────────────────────────
                val response: HttpResponse =
                    httpClient.request(targetUrl) {
                        this.method = method

                        val reqBuilder = this
                        call.request.headers.forEach { name, values ->
                            if (!name.equals("Host", ignoreCase = true) &&
                                !name.equals("Content-Length", ignoreCase = true)
                            ) {
                                values.forEach { reqBuilder.headers.append(name, it) }
                            }
                        }

                        if (userId != null) reqBuilder.headers["X-User-Id"] = userId
                        if (userRole != null) reqBuilder.headers["X-User-Role"] = userRole

                        if (body.isNotEmpty()) {
                            setBody(ByteArrayContent(body, call.request.contentType()))
                        }
                    }

                // ── Forward response ─────────────────────────────────────────
                val responseBody = response.readBytes()
                val contentType = response.contentType()

                call.response.status(response.status)
                response.headers.forEach { name, values ->
                    if (!name.equals("Transfer-Encoding", ignoreCase = true) &&
                        !name.equals("Content-Length", ignoreCase = true)
                    ) {
                        values.forEach { call.response.headers.append(name, it) }
                    }
                }

                if (contentType != null) {
                    call.respondBytes(responseBody, contentType, response.status)
                } else {
                    call.respondBytes(responseBody, status = response.status)
                }
            }
        }
    }
}
