package com.moodavatar.realtime.services

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomManagerTest {
    private val owner1 = "owner-1"
    private val owner2 = "owner-2"
    private val alice = RoomVisitor("user-alice", "alice", emotion = "happy")
    private val bob = RoomVisitor("user-bob", "bob", emotion = "sad")

    @AfterTest
    fun cleanup() {
        // Remove all test visitors from RoomManager to isolate tests
        RoomManager.leaveRoom(owner1, alice.userId)
        RoomManager.leaveRoom(owner1, bob.userId)
        RoomManager.leaveRoom(owner2, alice.userId)
        RoomManager.leaveRoom(owner2, bob.userId)
    }

    // ── joinRoom / getVisitors ────────────────────────────────────────────────

    @Test
    fun `joinRoom adds visitor to the owners room`() {
        RoomManager.joinRoom(owner1, alice)
        val visitors = RoomManager.getVisitors(owner1)
        assertEquals(1, visitors.size)
        assertEquals("alice", visitors.first().username)
    }

    @Test
    fun `joinRoom replaces existing entry if same visitor joins again`() {
        RoomManager.joinRoom(owner1, alice)
        val updated = alice.copy(emotion = "excited")
        RoomManager.joinRoom(owner1, updated)
        val visitors = RoomManager.getVisitors(owner1)
        assertEquals(1, visitors.size)
        assertEquals("excited", visitors.first().emotion)
    }

    @Test
    fun `joinRoom supports multiple visitors in the same room`() {
        RoomManager.joinRoom(owner1, alice)
        RoomManager.joinRoom(owner1, bob)
        assertEquals(2, RoomManager.getVisitors(owner1).size)
    }

    @Test
    fun `joinRoom creates separate rooms for different owners`() {
        RoomManager.joinRoom(owner1, alice)
        RoomManager.joinRoom(owner2, bob)
        assertEquals(1, RoomManager.getVisitors(owner1).size)
        assertEquals(1, RoomManager.getVisitors(owner2).size)
    }

    // ── getVisitors ───────────────────────────────────────────────────────────

    @Test
    fun `getVisitors returns empty list for a room with no visitors`() {
        assertTrue(RoomManager.getVisitors("no-such-owner").isEmpty())
    }

    // ── leaveRoom ─────────────────────────────────────────────────────────────

    @Test
    fun `leaveRoom removes the visitor and returns the visitor object`() {
        RoomManager.joinRoom(owner1, alice)
        val removed = RoomManager.leaveRoom(owner1, alice.userId)
        assertNotNull(removed)
        assertEquals("alice", removed.username)
        assertTrue(RoomManager.getVisitors(owner1).isEmpty())
    }

    @Test
    fun `leaveRoom returns null when visitor is not in the room`() {
        val result = RoomManager.leaveRoom(owner1, "nonexistent-user")
        assertNull(result)
    }

    @Test
    fun `leaveRoom cleans up empty room entry`() {
        RoomManager.joinRoom(owner1, alice)
        RoomManager.leaveRoom(owner1, alice.userId)
        // Room entry should be removed — getVisitors on an empty room returns []
        assertTrue(RoomManager.getVisitors(owner1).isEmpty())
    }

    // ── leaveAllRooms ─────────────────────────────────────────────────────────

    @Test
    fun `leaveAllRooms removes visitor from every room they are in`() {
        RoomManager.joinRoom(owner1, alice)
        RoomManager.joinRoom(owner2, alice)
        val results = RoomManager.leaveAllRooms(alice.userId)
        assertEquals(2, results.size)
        assertTrue(RoomManager.getVisitors(owner1).isEmpty())
        assertTrue(RoomManager.getVisitors(owner2).isEmpty())
    }

    @Test
    fun `leaveAllRooms returns empty list when user is not in any room`() {
        val results = RoomManager.leaveAllRooms("user-not-in-any-room")
        assertTrue(results.isEmpty())
    }

    @Test
    fun `leaveAllRooms returns pairs of ownerId and removed visitor`() {
        RoomManager.joinRoom(owner1, alice)
        val results = RoomManager.leaveAllRooms(alice.userId)
        assertEquals(owner1, results.first().first)
        assertEquals("alice", results.first().second.username)
    }

    @Test
    fun `leaveAllRooms does not remove other visitors from the room`() {
        RoomManager.joinRoom(owner1, alice)
        RoomManager.joinRoom(owner1, bob)
        RoomManager.leaveAllRooms(alice.userId)
        val remaining = RoomManager.getVisitors(owner1)
        assertEquals(1, remaining.size)
        assertEquals("bob", remaining.first().username)
    }
}
