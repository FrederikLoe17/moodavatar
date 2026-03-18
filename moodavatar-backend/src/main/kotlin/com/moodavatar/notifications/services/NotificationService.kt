package com.moodavatar.notifications.services

import com.moodavatar.notifications.dto.NotificationDto
import com.moodavatar.notifications.models.Notifications
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class NotificationService {
    fun create(
        userId: String,
        type: String,
        fromUserId: String,
        fromUsername: String,
        metadata: String? = null,
    ) = transaction {
        Notifications.insert {
            it[Notifications.userId]       = UUID.fromString(userId)
            it[Notifications.type]         = type
            it[Notifications.fromUserId]   = fromUserId
            it[Notifications.fromUsername] = fromUsername
            it[Notifications.read]         = false
            it[Notifications.metadata]     = metadata
            it[Notifications.createdAt]    = LocalDateTime.now()
        }
    }

    fun listForUser(userId: String, limit: Int = 50): List<NotificationDto> =
        transaction {
            Notifications
                .select { Notifications.userId eq UUID.fromString(userId) }
                .orderBy(Notifications.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toDto() }
        }

    fun unreadCount(userId: String): Int =
        transaction {
            Notifications
                .select {
                    (Notifications.userId eq UUID.fromString(userId)) and
                        (Notifications.read eq false)
                }
                .count().toInt()
        }

    fun markRead(id: String, userId: String): Boolean =
        transaction {
            Notifications.update({
                (Notifications.id eq UUID.fromString(id)) and
                    (Notifications.userId eq UUID.fromString(userId))
            }) {
                it[read] = true
            } > 0
        }

    fun markAllRead(userId: String) =
        transaction {
            Notifications.update({
                (Notifications.userId eq UUID.fromString(userId)) and
                    (Notifications.read eq false)
            }) {
                it[read] = true
            }
        }

    private fun ResultRow.toDto() = NotificationDto(
        id           = this[Notifications.id].toString(),
        type         = this[Notifications.type],
        fromUserId   = this[Notifications.fromUserId],
        fromUsername = this[Notifications.fromUsername],
        read         = this[Notifications.read],
        metadata     = this[Notifications.metadata],
        createdAt    = this[Notifications.createdAt].toString(),
    )
}
