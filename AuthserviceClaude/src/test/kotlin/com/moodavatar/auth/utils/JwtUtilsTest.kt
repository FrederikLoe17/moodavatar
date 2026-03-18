package com.moodavatar.auth.utils

import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtUtilsTest {
    private val secret = "test-secret-key-at-least-256-bits"
    private val issuer = "test-issuer"
    private val audience = "test-audience"

    // ── generate / decode ─────────────────────────────────────────────────────

    @Test
    fun `generate creates a non-blank JWT`() {
        val token =
            JwtUtils.generate(
                secret = secret,
                issuer = issuer,
                audience = audience,
                claims = mapOf("userId" to "user-1", "role" to "USER"),
                expiresAt = Date(System.currentTimeMillis() + 60_000),
            )
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `decode extracts custom claims correctly`() {
        val token =
            JwtUtils.generate(
                secret = secret,
                issuer = issuer,
                audience = audience,
                claims = mapOf("userId" to "abc-123", "role" to "ADMIN"),
                expiresAt = Date(System.currentTimeMillis() + 60_000),
            )
        val decoded = JwtUtils.decode(token)
        assertEquals("abc-123", decoded.getClaim("userId").asString())
        assertEquals("ADMIN", decoded.getClaim("role").asString())
        assertEquals(issuer, decoded.issuer)
    }

    @Test
    fun `decode returns the correct audience`() {
        val token =
            JwtUtils.generate(
                secret = secret,
                issuer = issuer,
                audience = audience,
                claims = emptyMap(),
                expiresAt = Date(System.currentTimeMillis() + 60_000),
            )
        val decoded = JwtUtils.decode(token)
        assertTrue(decoded.audience.contains(audience))
    }

    // ── isExpired ─────────────────────────────────────────────────────────────

    @Test
    fun `isExpired returns false for a freshly generated token`() {
        val token =
            JwtUtils.generate(
                secret = secret,
                issuer = issuer,
                audience = audience,
                claims = emptyMap(),
                expiresAt = Date(System.currentTimeMillis() + 60_000),
            )
        assertFalse(JwtUtils.isExpired(token))
    }

    @Test
    fun `isExpired returns true for an already-expired token`() {
        val token =
            JwtUtils.generate(
                secret = secret,
                issuer = issuer,
                audience = audience,
                claims = emptyMap(),
                expiresAt = Date(System.currentTimeMillis() - 1_000), // expired 1s ago
            )
        assertTrue(JwtUtils.isExpired(token))
    }

    @Test
    fun `isExpired returns true for a garbage string`() {
        assertTrue(JwtUtils.isExpired("not.a.jwt"))
    }
}
