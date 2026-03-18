package com.moodavatar.realtime.services

import io.ktor.websocket.WebSocketSession
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionManagerTest {

    private val aliceSession = mockk<WebSocketSession>(relaxed = true)
    private val bobSession = mockk<WebSocketSession>(relaxed = true)

    private val alice = ConnectedUser("user-alice", "alice", aliceSession)
    private val bob = ConnectedUser("user-bob", "bob", bobSession)

    @AfterTest
    fun cleanup() {
        ConnectionManager.disconnect(alice.userId)
        ConnectionManager.disconnect(bob.userId)
    }

    // ── connect / disconnect ──────────────────────────────────────────────────

    @Test
    fun `connect makes user online`() {
        ConnectionManager.connect(alice)
        assertTrue(ConnectionManager.isOnline(alice.userId))
    }

    @Test
    fun `disconnect makes user offline`() {
        ConnectionManager.connect(alice)
        ConnectionManager.disconnect(alice.userId)
        assertFalse(ConnectionManager.isOnline(alice.userId))
    }

    @Test
    fun `disconnect on unknown userId does not throw`() {
        ConnectionManager.disconnect("not-connected-user")
        assertFalse(ConnectionManager.isOnline("not-connected-user"))
    }

    // ── isOnline ──────────────────────────────────────────────────────────────

    @Test
    fun `isOnline returns false for user who has not connected`() {
        assertFalse(ConnectionManager.isOnline("never-connected"))
    }

    // ── getOnlineUserIds ──────────────────────────────────────────────────────

    @Test
    fun `getOnlineUserIds includes all connected users`() {
        ConnectionManager.connect(alice)
        ConnectionManager.connect(bob)
        val online = ConnectionManager.getOnlineUserIds()
        assertTrue(online.contains(alice.userId))
        assertTrue(online.contains(bob.userId))
    }

    @Test
    fun `getOnlineUserIds does not include disconnected users`() {
        ConnectionManager.connect(alice)
        ConnectionManager.disconnect(alice.userId)
        assertFalse(ConnectionManager.getOnlineUserIds().contains(alice.userId))
    }

    // ── sendTo ────────────────────────────────────────────────────────────────

    @Test
    fun `sendTo returns true and sends frame when user is connected`() = runBlocking {
        ConnectionManager.connect(alice)
        val result = ConnectionManager.sendTo(alice.userId, """{"type":"ping"}""")
        assertTrue(result)
    }

    @Test
    fun `sendTo returns false when user is not connected`() = runBlocking {
        val result = ConnectionManager.sendTo("offline-user", """{"type":"ping"}""")
        assertFalse(result)
    }

    @Test
    fun `sendTo returns false and removes user when session throws`() = runBlocking {
        val failingSession = mockk<WebSocketSession>(relaxed = true)
        coEvery { failingSession.send(any()) } throws Exception("Connection closed")
        val failingUser = ConnectedUser("failing-user", "failer", failingSession)
        ConnectionManager.connect(failingUser)
        val result = ConnectionManager.sendTo(failingUser.userId, "payload")
        assertFalse(result)
        assertFalse(ConnectionManager.isOnline(failingUser.userId))
    }

    // ── encode ────────────────────────────────────────────────────────────────

    @Test
    fun `encode serializes a data class to JSON string`() {
        val msg = mapOf("type" to "ping", "data" to "hello")
        val json = ConnectionManager.encode(msg)
        assertTrue(json.contains("ping"))
        assertTrue(json.contains("hello"))
    }
}
