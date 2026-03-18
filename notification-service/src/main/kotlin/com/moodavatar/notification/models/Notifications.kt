package com.moodavatar.notification.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Notifications : Table("notifications") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val type = varchar("type", 50)
    val fromUserId = varchar("from_user_id", 255)
    val fromUsername = varchar("from_username", 100)
    val read = bool("read").default(false)
    val metadata = text("metadata").nullable()
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}
