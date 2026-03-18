package com.moodavatar.user.services

import com.moodavatar.user.dto.*
import com.moodavatar.user.models.FriendRequests
import com.moodavatar.user.models.FriendshipStatus
import com.moodavatar.user.models.Profiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class FriendService(private val events: EventPublisher? = null) {

    fun sendRequest(senderId: UUID, receiverId: UUID): FriendRequestResponse {
        val result = transaction {
            if (senderId == receiverId) error("CANNOT_ADD_SELF")

            val exists = FriendRequests.select {
                (FriendRequests.senderId eq senderId) and
                        (FriendRequests.receiverId eq receiverId) and
                        (FriendRequests.status eq FriendshipStatus.PENDING)
            }.count() > 0
            if (exists) error("REQUEST_ALREADY_SENT")

            Profiles.select { Profiles.id eq receiverId }.singleOrNull()
                ?: error("USER_NOT_FOUND")

            val senderUsername = Profiles.select { Profiles.id eq senderId }
                .singleOrNull()?.get(Profiles.username) ?: senderId.toString()

            val now = LocalDateTime.now()
            val id  = UUID.randomUUID()
            FriendRequests.insert {
                it[FriendRequests.id]         = id
                it[FriendRequests.senderId]   = senderId
                it[FriendRequests.receiverId] = receiverId
                it[status]                    = FriendshipStatus.PENDING
                it[createdAt]                 = now
                it[updatedAt]                 = now
            }
            Triple(
                FriendRequestResponse(id.toString(), senderId.toString(), receiverId.toString(), FriendshipStatus.PENDING.name),
                senderUsername,
                receiverId.toString(),
            )
        }
        events?.publish(
            "friend.request.sent",
            """{"senderId":"${result.first.senderId}","senderUsername":"${result.second}","receiverId":"${result.third}"}"""
        )
        return result.first
    }

    fun respondToRequest(requestId: UUID, receiverId: UUID, action: String): FriendRequestResponse {
        val result = transaction {
            val row = FriendRequests.select {
                (FriendRequests.id eq requestId) and
                        (FriendRequests.receiverId eq receiverId)
            }.singleOrNull() ?: error("REQUEST_NOT_FOUND")

            if (row[FriendRequests.status] != FriendshipStatus.PENDING) error("REQUEST_ALREADY_HANDLED")

            val newStatus = when (action.uppercase()) {
                "ACCEPT"  -> FriendshipStatus.ACCEPTED
                "DECLINE" -> FriendshipStatus.DECLINED
                else      -> error("INVALID_ACTION")
            }

            FriendRequests.update({ FriendRequests.id eq requestId }) {
                it[status]    = newStatus
                it[updatedAt] = LocalDateTime.now()
            }

            val acceptorUsername = Profiles.select { Profiles.id eq receiverId }
                .singleOrNull()?.get(Profiles.username) ?: receiverId.toString()

            Triple(
                FriendRequestResponse(
                    requestId.toString(),
                    row[FriendRequests.senderId].toString(),
                    row[FriendRequests.receiverId].toString(),
                    newStatus.name,
                ),
                newStatus,
                acceptorUsername,
            )
        }
        if (result.second == FriendshipStatus.ACCEPTED) {
            events?.publish(
                "friend.request.accepted",
                """{"senderId":"${result.first.senderId}","acceptorId":"${result.first.receiverId}","acceptorUsername":"${result.third}"}"""
            )
        }
        return result.first
    }

    fun getFriends(userId: UUID): List<ProfileResponse> = transaction {
        val friendIds = FriendRequests.select {
            ((FriendRequests.senderId eq userId) or (FriendRequests.receiverId eq userId)) and
                    (FriendRequests.status eq FriendshipStatus.ACCEPTED)
        }.map { row ->
            val sid = row[FriendRequests.senderId]
            val rid = row[FriendRequests.receiverId]
            if (sid == userId) rid else sid
        }

        Profiles.select { Profiles.id inList friendIds }.map {
            ProfileResponse(
                id          = it[Profiles.id].toString(),
                username    = it[Profiles.username],
                displayName = it[Profiles.displayName],
                bio         = it[Profiles.bio],
                avatarUrl   = it[Profiles.avatarUrl]
            )
        }
    }

    fun getPendingRequests(userId: UUID): List<FriendRequestResponse> = transaction {
        FriendRequests
            .join(Profiles, JoinType.INNER, FriendRequests.senderId, Profiles.id)
            .select {
                (FriendRequests.receiverId eq userId) and
                (FriendRequests.status eq FriendshipStatus.PENDING)
            }.map {
                FriendRequestResponse(
                    id             = it[FriendRequests.id].toString(),
                    senderId       = it[FriendRequests.senderId].toString(),
                    receiverId     = it[FriendRequests.receiverId].toString(),
                    status         = it[FriendRequests.status].name,
                    senderUsername = it[Profiles.username],
                )
            }
    }

    fun removeFriend(userId: UUID, friendId: UUID): Boolean = transaction {
        val deleted = FriendRequests.deleteWhere {
            ((FriendRequests.senderId eq userId) and (FriendRequests.receiverId eq friendId)) or
                    ((FriendRequests.senderId eq friendId) and (FriendRequests.receiverId eq userId))
        }
        deleted > 0
    }
}