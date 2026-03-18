package com.moodavatar.auth.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.postgresql.util.PGobject

enum class Role { USER, ADMIN }

object Users : Table("users") {
    val id           = uuid("id").autoGenerate()
    val email        = varchar("email", 255).uniqueIndex()
    val username     = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = customEnumeration(
        name = "role",
        sql  = "user_role",
        fromDb = { value -> Role.valueOf(value as String) },
        toDb = {
            PGobject().apply {
                type  = "user_role"
                this.value = it.name
            }
        }
    ).default(Role.USER)
    val isVerified = bool("is_verified").default(false)
    val createdAt  = datetime("created_at")
    val updatedAt  = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
