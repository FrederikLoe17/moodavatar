package com.moodavatar.user.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.postgresql.util.PGobject

enum class FriendshipStatus { PENDING, ACCEPTED, DECLINED }

object Profiles : Table("profiles") {
    val id = uuid("id")
    val username = varchar("username", 100).uniqueIndex()
    val displayName = varchar("display_name", 100).nullable()
    val bio = text("bio").nullable()
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object FriendRequests : Table("friend_requests") {
    val id = uuid("id").autoGenerate()
    val senderId = uuid("sender_id").references(Profiles.id)
    val receiverId = uuid("receiver_id").references(Profiles.id)
    val status =
        customEnumeration(
            name = "status",
            sql = "friendship_status",
            fromDb = { value -> FriendshipStatus.valueOf(value as String) },
            toDb = {
                PGobject().apply {
                    type = "friendship_status"
                    this.value = it.name
                }
            },
        ).default(FriendshipStatus.PENDING)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}
