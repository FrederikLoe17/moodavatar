package com.moodavatar.user.services

import com.moodavatar.user.TestDatabase
import com.moodavatar.user.dto.CreateProfileRequest
import com.moodavatar.user.dto.UpdateProfileRequest
import com.moodavatar.user.models.FriendRequests
import com.moodavatar.user.models.Profiles
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileServiceTest {

    private val db = TestDatabase
    private val service = ProfileService()

    @BeforeTest
    fun cleanDb() = transaction {
        FriendRequests.deleteAll()
        Profiles.deleteAll()
    }

    // ── createProfile ─────────────────────────────────────────────────────────

    @Test
    fun `createProfile stores profile and returns it`() {
        val id = UUID.randomUUID()
        val result = service.createProfile(CreateProfileRequest(id.toString(), "alice", "Alice A"))
        assertEquals(id.toString(), result.id)
        assertEquals("alice", result.username)
        assertEquals("Alice A", result.displayName)
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    fun `getProfile returns null for unknown id`() {
        assertNull(service.getProfile(UUID.randomUUID()))
    }

    @Test
    fun `getProfile returns profile for existing id`() {
        val id = UUID.randomUUID()
        service.createProfile(CreateProfileRequest(id.toString(), "bob", "Bob B"))
        val result = service.getProfile(id)
        assertNotNull(result)
        assertEquals("bob", result.username)
    }

    // ── getProfileByUsername ──────────────────────────────────────────────────

    @Test
    fun `getProfileByUsername returns null for unknown username`() {
        assertNull(service.getProfileByUsername("nobody"))
    }

    @Test
    fun `getProfileByUsername returns profile for existing username`() {
        val id = UUID.randomUUID()
        service.createProfile(CreateProfileRequest(id.toString(), "carol", "Carol C"))
        val result = service.getProfileByUsername("carol")
        assertNotNull(result)
        assertEquals(id.toString(), result.id)
    }

    // ── searchProfiles ────────────────────────────────────────────────────────

    @Test
    fun `searchProfiles returns matching profiles by username substring`() {
        service.createProfile(CreateProfileRequest(UUID.randomUUID().toString(), "david", "David"))
        service.createProfile(CreateProfileRequest(UUID.randomUUID().toString(), "dave2", "Dave2"))
        service.createProfile(CreateProfileRequest(UUID.randomUUID().toString(), "eve", "Eve"))

        val results = service.searchProfiles("dav")
        assertEquals(2, results.size)
        assertTrue(results.all { it.username.contains("dav") })
    }

    @Test
    fun `searchProfiles returns empty list when no match`() {
        assertTrue(service.searchProfiles("zzznomatch").isEmpty())
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    fun `updateProfile changes displayName and bio`() {
        val id = UUID.randomUUID()
        service.createProfile(CreateProfileRequest(id.toString(), "frank", "Frank"))
        val updated = service.updateProfile(id, UpdateProfileRequest(displayName = "Frank Updated", bio = "Hello!"))
        assertNotNull(updated)
        assertEquals("Frank Updated", updated.displayName)
        assertEquals("Hello!", updated.bio)
    }

    @Test
    fun `updateProfile returns null for unknown id`() {
        val result = service.updateProfile(UUID.randomUUID(), UpdateProfileRequest(displayName = "X"))
        assertNull(result)
    }

    @Test
    fun `updateProfile with null fields does not overwrite existing values`() {
        val id = UUID.randomUUID()
        service.createProfile(CreateProfileRequest(id.toString(), "grace", "Grace"))
        service.updateProfile(id, UpdateProfileRequest(bio = "My bio"))
        val updated = service.updateProfile(id, UpdateProfileRequest(displayName = "Grace New"))
        assertNotNull(updated)
        assertEquals("Grace New", updated.displayName)
        assertEquals("My bio", updated.bio) // unchanged
    }
}
