package com.moodavatar.gateway.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class RateLimiter(private val maxPerMinute: Int = 120) {

    private data class Bucket(val count: AtomicInteger, val resetAt: Long)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun isAllowed(ip: String): Boolean {
        val now = System.currentTimeMillis()
        val bucket = buckets.compute(ip) { _, existing ->
            if (existing == null || now >= existing.resetAt)
                Bucket(AtomicInteger(1), now + 60_000)
            else {
                existing.count.incrementAndGet()
                existing
            }
        }!!
        return bucket.count.get() <= maxPerMinute
    }

    fun remaining(ip: String): Int {
        val bucket = buckets[ip] ?: return maxPerMinute
        return maxOf(0, maxPerMinute - bucket.count.get())
    }
}

@Serializable
data class RateLimitError(val error: String, val message: String)

val RateLimitPlugin = createApplicationPlugin("RateLimit", { }) {
    val limiter = RateLimiter(
        maxPerMinute = application.environment.config
            .propertyOrNull("gateway.rateLimitPerMin")?.getString()?.toIntOrNull() ?: 120
    )

    onCall { call ->
        val ip = call.request.headers["X-Forwarded-For"]?.split(",")?.first()?.trim()
            ?: call.request.local.remoteHost

        if (!limiter.isAllowed(ip)) {
            call.response.headers.append("X-RateLimit-Limit",     "120")
            call.response.headers.append("X-RateLimit-Remaining", "0")
            call.respond(
                HttpStatusCode.TooManyRequests,
                RateLimitError("RATE_LIMIT_EXCEEDED", "Too many requests. Try again in a minute.")
            )
            return@onCall
        }
        call.response.headers.append("X-RateLimit-Remaining", limiter.remaining(ip).toString())
    }
}
