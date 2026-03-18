package com.moodavatar.auth.services

import com.moodavatar.auth.TestDatabase
import com.moodavatar.auth.dto.LoginRequest
import com.moodavatar.auth.dto.RefreshRequest
import com.moodavatar.auth.dto.RegisterRequest
import com.moodavatar.auth.models.EmailVerifications
import com.moodavatar.auth.models.PasswordResets
import com.moodavatar.auth.models.RefreshTokens
import com.moodavatar.auth.models.Users
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTest {
    private val db = TestDatabase // ensures container starts
    private val service = AuthService(TestDatabase.config)

    @BeforeTest
    fun cleanDb() =
        transaction {
            EmailVerifications.deleteAll()
            PasswordResets.deleteAll()
            RefreshTokens.deleteAll()
            Users.deleteAll()
        }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    fun `register creates user and returns USER role by default`() {
        val result = service.register(RegisterRequest("new@example.com", "newuser", "Password1"))
        assertEquals("new@example.com", result.email)
        assertEquals("newuser", result.username)
        assertEquals("USER", result.role)
        assertFalse(result.isVerified)
    }

    @Test
    fun `register throws EMAIL_TAKEN when email already exists`() {
        service.register(RegisterRequest("taken@example.com", "user1", "Password1"))
        val ex =
            assertFailsWith<IllegalStateException> {
                service.register(RegisterRequest("taken@example.com", "user2", "Password1"))
            }
        assertEquals("EMAIL_TAKEN", ex.message)
    }

    @Test
    fun `register throws USERNAME_TAKEN when username already exists`() {
        service.register(RegisterRequest("a@example.com", "sameuser", "Password1"))
        val ex =
            assertFailsWith<IllegalStateException> {
                service.register(RegisterRequest("b@example.com", "sameuser", "Password1"))
            }
        assertEquals("USERNAME_TAKEN", ex.message)
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login returns access and refresh tokens for valid credentials`() {
        service.register(RegisterRequest("login@example.com", "loginuser", "Password1"))
        val result = service.login(LoginRequest("login@example.com", "Password1"))
        assertTrue(result.accessToken.isNotBlank())
        assertTrue(result.refreshToken.isNotBlank())
        assertEquals("Bearer", result.tokenType)
        assertEquals("login@example.com", result.user.email)
    }

    @Test
    fun `login throws INVALID_CREDENTIALS for wrong password`() {
        service.register(RegisterRequest("pw@example.com", "pwuser", "Password1"))
        val ex =
            assertFailsWith<IllegalStateException> {
                service.login(LoginRequest("pw@example.com", "WrongPassword1"))
            }
        assertEquals("INVALID_CREDENTIALS", ex.message)
    }

    @Test
    fun `login throws INVALID_CREDENTIALS for unknown email`() {
        val ex =
            assertFailsWith<IllegalStateException> {
                service.login(LoginRequest("nobody@example.com", "Password1"))
            }
        assertEquals("INVALID_CREDENTIALS", ex.message)
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `refresh returns new access token for valid refresh token`() {
        service.register(RegisterRequest("refresh@example.com", "refreshuser", "Password1"))
        val auth = service.login(LoginRequest("refresh@example.com", "Password1"))
        val result = service.refresh(RefreshRequest(auth.refreshToken))
        assertTrue(result.accessToken.isNotBlank())
    }

    @Test
    fun `refresh throws INVALID_REFRESH_TOKEN for unknown token`() {
        val ex =
            assertFailsWith<IllegalStateException> {
                service.refresh(RefreshRequest("not-a-valid-token"))
            }
        assertEquals("INVALID_REFRESH_TOKEN", ex.message)
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    fun `logout revokes token so refresh fails with TOKEN_REVOKED`() {
        service.register(RegisterRequest("logout@example.com", "logoutuser", "Password1"))
        val auth = service.login(LoginRequest("logout@example.com", "Password1"))
        service.logout(auth.refreshToken)
        val ex =
            assertFailsWith<IllegalStateException> {
                service.refresh(RefreshRequest(auth.refreshToken))
            }
        assertEquals("TOKEN_REVOKED", ex.message)
    }

    // ── password reset ────────────────────────────────────────────────────────

    @Test
    fun `createPasswordResetToken returns null for unknown email`() {
        assertNull(service.createPasswordResetToken("ghost@example.com"))
    }

    @Test
    fun `createPasswordResetToken returns email and token pair for known user`() {
        service.register(RegisterRequest("reset@example.com", "resetuser", "Password1"))
        val result = service.createPasswordResetToken("reset@example.com")
        assertNotNull(result)
        assertEquals("reset@example.com", result.first)
        assertTrue(result.second.isNotBlank())
    }

    @Test
    fun `resetPassword allows login with new password after reset`() {
        service.register(RegisterRequest("newpw@example.com", "newpwuser", "OldPassword1"))
        val (_, token) = service.createPasswordResetToken("newpw@example.com")!!
        service.resetPassword(token, "NewPassword1")
        val auth = service.login(LoginRequest("newpw@example.com", "NewPassword1"))
        assertTrue(auth.accessToken.isNotBlank())
    }

    @Test
    fun `resetPassword throws TOKEN_ALREADY_USED when token is reused`() {
        service.register(RegisterRequest("reuse@example.com", "reuseuser", "OldPassword1"))
        val (_, token) = service.createPasswordResetToken("reuse@example.com")!!
        service.resetPassword(token, "NewPassword1")
        val ex =
            assertFailsWith<IllegalStateException> {
                service.resetPassword(token, "AnotherPassword1")
            }
        assertEquals("TOKEN_ALREADY_USED", ex.message)
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    fun `getUserById returns null for unknown id`() {
        assertNull(service.getUserById(UUID.randomUUID()))
    }

    @Test
    fun `getUserById returns user for known id`() {
        val registered = service.register(RegisterRequest("getme@example.com", "getmeuser", "Password1"))
        val found = service.getUserById(UUID.fromString(registered.id))
        assertNotNull(found)
        assertEquals("getme@example.com", found.email)
    }

    // ── email verification ────────────────────────────────────────────────────

    @Test
    fun `verifyEmail marks user as verified`() {
        val registered = service.register(RegisterRequest("verify@example.com", "verifyuser", "Password1"))
        val token = service.createVerificationToken(UUID.fromString(registered.id))
        service.verifyEmail(token)
        val user = service.getUserById(UUID.fromString(registered.id))
        assertNotNull(user)
        assertTrue(user.isVerified)
    }

    @Test
    fun `verifyEmail throws TOKEN_ALREADY_USED on second call`() {
        val registered = service.register(RegisterRequest("verify2@example.com", "verifyuser2", "Password1"))
        val token = service.createVerificationToken(UUID.fromString(registered.id))
        service.verifyEmail(token)
        val ex = assertFailsWith<IllegalStateException> { service.verifyEmail(token) }
        assertEquals("TOKEN_ALREADY_USED", ex.message)
    }
}
