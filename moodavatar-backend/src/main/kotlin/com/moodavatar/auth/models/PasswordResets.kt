package com.moodavatar.auth.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object PasswordResets : Table("password_resets") {
    val id        = uuid("id").autoGenerate()
    val userId    = uuid("user_id").references(Users.id)
    val token     = varchar("token", 512).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val used      = bool("used").default(false)

    override val primaryKey = PrimaryKey(id)
}
