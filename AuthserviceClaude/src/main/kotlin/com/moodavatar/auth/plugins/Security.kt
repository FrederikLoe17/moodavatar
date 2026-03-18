package com.moodavatar.auth.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity() {
    val cfg      = environment.config
    val secret   = cfg.property("jwt.secret").getString()
    val issuer   = cfg.property("jwt.issuer").getString()
    val audience = cfg.property("jwt.audience").getString()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "moodavatar"
            verifier(
                JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
        }
    }
}