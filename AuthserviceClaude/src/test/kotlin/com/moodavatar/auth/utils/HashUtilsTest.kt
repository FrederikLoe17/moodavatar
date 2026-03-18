package com.moodavatar.auth.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HashUtilsTest {

    // ── hashPassword / verifyPassword ─────────────────────────────────────────

    @Test
    fun `hashPassword returns a non-empty hash different from plaintext`() {
        val hash = HashUtils.hashPassword("Secret123")
        assertTrue(hash.isNotBlank())
        assertNotEquals("Secret123", hash)
    }

    @Test
    fun `hashPassword produces different hashes for the same input (salt)`() {
        val hash1 = HashUtils.hashPassword("Secret123")
        val hash2 = HashUtils.hashPassword("Secret123")
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `verifyPassword returns true for correct password`() {
        val hash = HashUtils.hashPassword("MyPassword1")
        assertTrue(HashUtils.verifyPassword("MyPassword1", hash))
    }

    @Test
    fun `verifyPassword returns false for wrong password`() {
        val hash = HashUtils.hashPassword("MyPassword1")
        assertFalse(HashUtils.verifyPassword("WrongPassword1", hash))
    }

    @Test
    fun `verifyPassword returns false for malformed hash`() {
        assertFalse(HashUtils.verifyPassword("anyPassword", "not-a-bcrypt-hash"))
    }

    // ── isStrongPassword ──────────────────────────────────────────────────────

    @Test
    fun `isStrongPassword returns true for valid strong password`() {
        assertTrue(HashUtils.isStrongPassword("Secret123"))
    }

    @Test
    fun `isStrongPassword returns false when shorter than 8 chars`() {
        assertFalse(HashUtils.isStrongPassword("Short1"))
    }

    @Test
    fun `isStrongPassword returns false when no digit`() {
        assertFalse(HashUtils.isStrongPassword("NoDigitHere"))
    }

    @Test
    fun `isStrongPassword returns false when no uppercase`() {
        assertFalse(HashUtils.isStrongPassword("nouppercase1"))
    }

    // ── generateSecureToken ───────────────────────────────────────────────────

    @Test
    fun `generateSecureToken returns a non-blank UUID-shaped string`() {
        val token = HashUtils.generateSecureToken()
        assertTrue(token.isNotBlank())
        assertEquals(36, token.length) // UUID format: 8-4-4-4-12
    }

    @Test
    fun `generateSecureToken produces unique values`() {
        val t1 = HashUtils.generateSecureToken()
        val t2 = HashUtils.generateSecureToken()
        assertNotEquals(t1, t2)
    }

    // ── sha256 ────────────────────────────────────────────────────────────────

    @Test
    fun `sha256 is deterministic for the same input`() {
        val h1 = HashUtils.sha256("hello")
        val h2 = HashUtils.sha256("hello")
        assertEquals(h1, h2)
    }

    @Test
    fun `sha256 produces different hashes for different inputs`() {
        assertNotEquals(HashUtils.sha256("hello"), HashUtils.sha256("world"))
    }

    @Test
    fun `sha256 produces a 64-character hex string`() {
        val hash = HashUtils.sha256("test")
        assertNotNull(hash)
        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }
}
