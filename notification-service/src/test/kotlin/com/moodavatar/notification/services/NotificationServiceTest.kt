package com.moodavatar.notification.services

import com.moodavatar.notification.models.Notifications
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationServiceTest {

    private lateinit var service: NotificationService
    private val userId = UUID.randomUUID().toString()
    private val fromUserId = UUID.randomUUID().toString()

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:h2:mem:notification_test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(Notifications) }
        service = NotificationService()
    }

    @AfterTest
    fun cleanDb() = transaction {
        Notifications.deleteAll()
    }

    private fun createNotification(
        type: String = "FRIEND_REQUEST",
        forUser: String = userId,
    ) = service.create(
        userId = forUser,
        type = type,
        fromUserId = fromUserId,
        fromUsername = "sender",
    )

    // ── create / listForUser ──────────────────────────────────────────────────

    @Test
    fun `create inserts a notification visible in listForUser`() {
        createNotification()
        val list = service.listForUser(userId)
        assertEquals(1, list.size)
        assertEquals("FRIEND_REQUEST", list.first().type)
        assertEquals(fromUserId, list.first().fromUserId)
        assertFalse(list.first().read)
    }

    @Test
    fun `listForUser returns empty list for user with no notifications`() {
        assertTrue(service.listForUser(UUID.randomUUID().toString()).isEmpty())
    }

    @Test
    fun `listForUser returns notifications in descending order`() {
        createNotification(type = "TYPE_A")
        createNotification(type = "TYPE_B")
        val list = service.listForUser(userId)
        assertEquals("TYPE_B", list.first().type)
        assertEquals("TYPE_A", list.last().type)
    }

    @Test
    fun `listForUser does not return notifications of other users`() {
        val otherUser = UUID.randomUUID().toString()
        createNotification(forUser = otherUser)
        assertTrue(service.listForUser(userId).isEmpty())
    }

    // ── unreadCount ───────────────────────────────────────────────────────────

    @Test
    fun `unreadCount returns correct count of unread notifications`() {
        createNotification()
        createNotification()
        assertEquals(2, service.unreadCount(userId))
    }

    @Test
    fun `unreadCount returns 0 for user with no notifications`() {
        assertEquals(0, service.unreadCount(UUID.randomUUID().toString()))
    }

    // ── markRead ──────────────────────────────────────────────────────────────

    @Test
    fun `markRead returns true and marks notification as read`() {
        createNotification()
        val id = service.listForUser(userId).first().id
        val result = service.markRead(id, userId)
        assertTrue(result)
        assertEquals(0, service.unreadCount(userId))
    }

    @Test
    fun `markRead returns false for unknown notification id`() {
        val result = service.markRead(UUID.randomUUID().toString(), userId)
        assertFalse(result)
    }

    @Test
    fun `markRead returns false when notification belongs to different user`() {
        val otherUser = UUID.randomUUID().toString()
        createNotification(forUser = otherUser)
        val id = service.listForUser(otherUser).first().id
        val result = service.markRead(id, userId)
        assertFalse(result)
    }

    // ── markAllRead ───────────────────────────────────────────────────────────

    @Test
    fun `markAllRead sets all notifications to read for user`() {
        createNotification()
        createNotification()
        service.markAllRead(userId)
        assertEquals(0, service.unreadCount(userId))
    }

    @Test
    fun `markAllRead does not affect other users notifications`() {
        val otherUser = UUID.randomUUID().toString()
        createNotification()
        createNotification(forUser = otherUser)
        service.markAllRead(userId)
        assertEquals(1, service.unreadCount(otherUser))
    }
}
