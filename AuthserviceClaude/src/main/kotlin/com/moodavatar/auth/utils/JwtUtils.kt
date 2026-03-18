package com.moodavatar.auth.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.auth.jwt.*

object JwtUtils {
    /**
     * Extrahiert die userId aus einem JWTPrincipal (für geschützte Routen)
     */
    fun JWTPrincipal.userId(): String =
        payload.getClaim("userId").asString()
            ?: error("userId claim missing in JWT")

    /**
     * Extrahiert die Rolle aus einem JWTPrincipal
     */
    fun JWTPrincipal.userRole(): String =
        payload.getClaim("role").asString()
            ?: error("role claim missing in JWT")

    /**
     * Dekodiert einen JWT-Token ohne Verifikation (z.B. für Logging)
     */
    fun decode(token: String): DecodedJWT = JWT.decode(token)

    /**
     * Prüft ob ein Token abgelaufen ist (ohne Verifikation)
     */
    fun isExpired(token: String): Boolean =
        runCatching {
            JWT.decode(token).expiresAt.before(java.util.Date())
        }.getOrDefault(true)

    /**
     * Generiert einen signierten JWT (wird vom AuthService genutzt,
     * hier als wiederverwendbare Hilfsfunktion)
     */
    fun generate(
        secret: String,
        issuer: String,
        audience: String,
        claims: Map<String, String>,
        expiresAt: java.util.Date,
    ): String {
        var builder =
            JWT
                .create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withExpiresAt(expiresAt)

        claims.forEach { (k, v) -> builder = builder.withClaim(k, v) }

        return builder.sign(Algorithm.HMAC256(secret))
    }
}
