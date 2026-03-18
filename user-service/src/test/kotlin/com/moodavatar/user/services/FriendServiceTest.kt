package com.moodavatar.user.services

import com.moodavatar.user.TestDatabase
import com.moodavatar.user.dto.CreateProfileRequest
import com.moodavatar.user.models.FriendRequests
import com.moodavatar.user.models.FriendshipStatus
import com.moodavatar.user.models.Profiles
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FriendServiceTest {
    private val db = TestDatabase
    private val profileService = ProfileService()
    private val friendService = FriendService(events = null)

    private lateinit var aliceId: UUID
    private lateinit var bobId: UUID
    private lateinit var carolId: UUID

    @BeforeTest
    fun setup() {
        transaction {
            FriendRequests.deleteAll()
            Profiles.deleteAll()
        }
        aliceId = UUID.randomUUID()
        bobId = UUID.randomUUID()
        carolId = UUID.randomUUID()
        profileService.createProfile(CreateProfileRequest(aliceId.toString(), "alice", "Alice"))
        profileService.createProfile(CreateProfileRequest(bobId.toString(), "bob", "Bob"))
        profileService.createProfile(CreateProfileRequest(carolId.toString(), "carol", "Carol"))
    }

    // ── sendRequest ───────────────────────────────────────────────────────────

    @Test
    fun `sendRequest creates a PENDING friend request`() {
        val result = friendService.sendRequest(aliceId, bobId)
        assertEquals(aliceId.toString(), result.senderId)
        assertEquals(bobId.toString(), result.receiverId)
        assertEquals(FriendshipStatus.PENDING.name, result.status)
    }

    @Test
    fun `sendRequest throws CANNOT_ADD_SELF when sender equals receiver`() {
        val ex =
            assertFailsWith<IllegalStateException> {
                friendService.sendRequest(aliceId, aliceId)
            }
        assertEquals("CANNOT_ADD_SELF", ex.message)
    }

    @Test
    fun `sendRequest throws REQUEST_ALREADY_SENT for duplicate pending request`() {
        friendService.sendRequest(aliceId, bobId)
        val ex =
            assertFailsWith<IllegalStateException> {
                friendService.sendRequest(aliceId, bobId)
            }
        assertEquals("REQUEST_ALREADY_SENT", ex.message)
    }

    @Test
    fun `sendRequest throws USER_NOT_FOUND for unknown receiver`() {
        val ex =
            assertFailsWith<IllegalStateException> {
                friendService.sendRequest(aliceId, UUID.randomUUID())
            }
        assertEquals("USER_NOT_FOUND", ex.message)
    }

    // ── respondToRequest ──────────────────────────────────────────────────────

    @Test
    fun `respondToRequest with ACCEPT changes status to ACCEPTED`() {
        val request = friendService.sendRequest(aliceId, bobId)
        val result = friendService.respondToRequest(UUID.fromString(request.id), bobId, "ACCEPT")
        assertEquals(FriendshipStatus.ACCEPTED.name, result.status)
    }

    @Test
    fun `respondToRequest with DECLINE changes status to DECLINED`() {
        val request = friendService.sendRequest(aliceId, bobId)
        val result = friendService.respondToRequest(UUID.fromString(request.id), bobId, "DECLINE")
        assertEquals(FriendshipStatus.DECLINED.name, result.status)
    }

    @Test
    fun `respondToRequest throws REQUEST_NOT_FOUND for wrong receiver`() {
        val request = friendService.sendRequest(aliceId, bobId)
        val ex =
            assertFailsWith<IllegalStateException> {
                friendService.respondToRequest(UUID.fromString(request.id), carolId, "ACCEPT")
            }
        assertEquals("REQUEST_NOT_FOUND", ex.message)
    }

    @Test
    fun `respondToRequest throws REQUEST_ALREADY_HANDLED when request is not PENDING`() {
        val request = friendService.sendRequest(aliceId, bobId)
        friendService.respondToRequest(UUID.fromString(request.id), bobId, "ACCEPT")
        val ex =
            assertFailsWith<IllegalStateException> {
                friendService.respondToRequest(UUID.fromString(request.id), bobId, "ACCEPT")
            }
        assertEquals("REQUEST_ALREADY_HANDLED", ex.message)
    }

    @Test
    fun `respondToRequest throws INVALID_ACTION for unknown action`() {
        val request = friendService.sendRequest(aliceId, bobId)
        val ex =
            assertFailsWith<IllegalStateException> {
                friendService.respondToRequest(UUID.fromString(request.id), bobId, "MAYBE")
            }
        assertEquals("INVALID_ACTION", ex.message)
    }

    // ── getFriends ────────────────────────────────────────────────────────────

    @Test
    fun `getFriends returns empty list when user has no accepted friends`() {
        val friends = friendService.getFriends(aliceId)
        assertTrue(friends.isEmpty())
    }

    @Test
    fun `getFriends returns accepted friends from both sender and receiver side`() {
        val req1 = friendService.sendRequest(aliceId, bobId)
        friendService.respondToRequest(UUID.fromString(req1.id), bobId, "ACCEPT")
        val req2 = friendService.sendRequest(carolId, aliceId)
        friendService.respondToRequest(UUID.fromString(req2.id), aliceId, "ACCEPT")

        val friends = friendService.getFriends(aliceId)
        assertEquals(2, friends.size)
        val usernames = friends.map { it.username }.toSet()
        assertTrue(usernames.contains("bob"))
        assertTrue(usernames.contains("carol"))
    }

    @Test
    fun `getFriends does not include declined or pending requests`() {
        val req = friendService.sendRequest(aliceId, bobId)
        friendService.respondToRequest(UUID.fromString(req.id), bobId, "DECLINE")
        friendService.sendRequest(aliceId, carolId) // pending

        val friends = friendService.getFriends(aliceId)
        assertTrue(friends.isEmpty())
    }

    // ── getPendingRequests ────────────────────────────────────────────────────

    @Test
    fun `getPendingRequests returns only incoming pending requests for receiver`() {
        friendService.sendRequest(aliceId, bobId)
        friendService.sendRequest(carolId, bobId)

        val pending = friendService.getPendingRequests(bobId)
        assertEquals(2, pending.size)
        val senders = pending.map { it.senderId }.toSet()
        assertTrue(senders.contains(aliceId.toString()))
        assertTrue(senders.contains(carolId.toString()))
    }

    @Test
    fun `getPendingRequests returns empty list when no pending requests`() {
        val pending = friendService.getPendingRequests(aliceId)
        assertTrue(pending.isEmpty())
    }

    // ── removeFriend ──────────────────────────────────────────────────────────

    @Test
    fun `removeFriend returns true and removes accepted friendship`() {
        val req = friendService.sendRequest(aliceId, bobId)
        friendService.respondToRequest(UUID.fromString(req.id), bobId, "ACCEPT")

        val removed = friendService.removeFriend(aliceId, bobId)
        assertTrue(removed)
        assertTrue(friendService.getFriends(aliceId).isEmpty())
    }

    @Test
    fun `removeFriend returns false when no friendship exists`() {
        val removed = friendService.removeFriend(aliceId, carolId)
        assertFalse(removed)
    }
}
