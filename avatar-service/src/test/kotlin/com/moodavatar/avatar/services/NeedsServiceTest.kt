package com.moodavatar.avatar.services

import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.bson.Document
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NeedsServiceTest {
    private val mockDb = mockk<MongoDatabase>()
    private val mockCollection = mockk<MongoCollection<Document>>()
    private lateinit var service: NeedsService

    @BeforeTest
    fun setup() {
        every { mockDb.getCollection("avatar_needs") } returns mockCollection
        service = NeedsService(mockDb)
    }

    private fun stubFind(doc: Document?) {
        val mockIterable = mockk<FindIterable<Document>>()
        every { mockCollection.find(any<Document>()) } returns mockIterable
        every { mockIterable.firstOrNull() } returns doc
    }

    // ── no record (doc == null) ───────────────────────────────────────────────

    @Test
    fun `getNeeds returns 30 for all stats when no record exists`() =
        runBlocking {
            stubFind(null)
            val needs = service.getNeeds("user-1")
            assertEquals(30, needs.mood)
            assertEquals(30, needs.energy)
            assertEquals(30, needs.social)
            assertEquals(30, needs.activity)
        }

    // ── fresh checkin (< 1 minute ago) ────────────────────────────────────────

    @Test
    fun `getNeeds returns 100 for mood and energy right after mood checkin`() =
        runBlocking {
            val now = Instant.now().toString()
            stubFind(Document("_id", "user-1").append("lastMoodCheckin", now))
            val needs = service.getNeeds("user-1")
            assertEquals(100, needs.mood)
            assertEquals(100, needs.energy)
            assertEquals(30, needs.social) // no social event
            assertEquals(30, needs.activity) // no lastActivity
        }

    // ── 20 hours ago → mood fully decayed ────────────────────────────────────

    @Test
    fun `getNeeds returns 0 for mood after 20 hours since last checkin`() =
        runBlocking {
            val twentyHoursAgo = Instant.now().minus(20, ChronoUnit.HOURS).toString()
            stubFind(Document("_id", "user-1").append("lastMoodCheckin", twentyHoursAgo))
            val needs = service.getNeeds("user-1")
            // mood: 100 - 20 * 5.0 = 0
            assertEquals(0, needs.mood)
            // energy: 100 - 20 * 2.08 = 58.4 → 58
            assertEquals(58, needs.energy)
        }

    // ── social event tracking ─────────────────────────────────────────────────

    @Test
    fun `getNeeds returns 100 for social right after social event`() =
        runBlocking {
            val now = Instant.now().toString()
            stubFind(Document("_id", "user-1").append("lastSocialEvent", now))
            val needs = service.getNeeds("user-1")
            assertEquals(30, needs.mood) // no mood checkin
            assertEquals(100, needs.social)
        }

    @Test
    fun `getNeeds returns 0 for social after 40 hours since last social event`() =
        runBlocking {
            // social: 100 - 40 * 2.78 = 100 - 111.2 → clamped to 0
            val fortyHoursAgo = Instant.now().minus(40, ChronoUnit.HOURS).toString()
            stubFind(Document("_id", "user-1").append("lastSocialEvent", fortyHoursAgo))
            val needs = service.getNeeds("user-1")
            assertEquals(0, needs.social)
        }

    // ── activity tracking ────────────────────────────────────────────────────

    @Test
    fun `getNeeds returns 100 for activity right after activity event`() =
        runBlocking {
            val now = Instant.now().toString()
            stubFind(Document("_id", "user-1").append("lastActivity", now))
            val needs = service.getNeeds("user-1")
            assertEquals(100, needs.activity)
        }

    @Test
    fun `getNeeds clamps all values between 0 and 100`() =
        runBlocking {
            // 30 hours ago → activity fully decayed (100 - 30*3.57 = -7 → clamped to 0)
            val thirtyHoursAgo = Instant.now().minus(30, ChronoUnit.HOURS).toString()
            stubFind(
                Document("_id", "user-1")
                    .append("lastMoodCheckin", thirtyHoursAgo)
                    .append("lastSocialEvent", thirtyHoursAgo)
                    .append("lastActivity", thirtyHoursAgo),
            )
            val needs = service.getNeeds("user-1")
            assertTrue(needs.mood in 0..100)
            assertTrue(needs.energy in 0..100)
            assertTrue(needs.social in 0..100)
            assertTrue(needs.activity in 0..100)
        }

    // ── onMoodCheckin / onSocialEvent ─────────────────────────────────────────

    @Test
    fun `onMoodCheckin calls updateOne on the collection`() =
        runBlocking {
            every { mockCollection.updateOne(any<Document>(), any<Document>(), any()) } returns mockk()
            service.onMoodCheckin("user-1")
            verify(exactly = 1) { mockCollection.updateOne(any<Document>(), any<Document>(), any()) }
        }

    @Test
    fun `onSocialEvent calls updateOne on the collection`() =
        runBlocking {
            every { mockCollection.updateOne(any<Document>(), any<Document>(), any()) } returns mockk()
            service.onSocialEvent("user-1")
            verify(exactly = 1) { mockCollection.updateOne(any<Document>(), any<Document>(), any()) }
        }
}
