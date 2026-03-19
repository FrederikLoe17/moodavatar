package com.moodavatar.auth.services

import com.moodavatar.auth.dto.AdminStatsResponse
import com.moodavatar.auth.dto.AdminUserResponse
import com.moodavatar.auth.dto.AdminUsersResponse
import com.moodavatar.auth.models.Role
import com.moodavatar.auth.models.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class AdminService {
    fun getStats(): AdminStatsResponse =
        transaction {
            val now = LocalDateTime.now()
            val todayStart = now.toLocalDate().atStartOfDay()
            val weekStart = now.toLocalDate().minusDays(7).atStartOfDay()

            AdminStatsResponse(
                totalUsers = Users.selectAll().count(),
                usersToday = Users.select { Users.createdAt greaterEq todayStart }.count(),
                usersThisWeek = Users.select { Users.createdAt greaterEq weekStart }.count(),
                adminCount = Users.select { Users.role eq Role.ADMIN }.count(),
                verifiedCount = Users.select { Users.isVerified eq true }.count(),
            )
        }

    fun listUsers(
        page: Int,
        pageSize: Int,
        search: String?,
    ): AdminUsersResponse =
        transaction {
            val base =
                if (search.isNullOrBlank()) {
                    Users.selectAll()
                } else {
                    Users.select { (Users.username like "%$search%") or (Users.email like "%$search%") }
                }
            val total = base.count()
            val users =
                base
                    .orderBy(Users.createdAt, SortOrder.DESC)
                    .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                    .map { it.toAdminResponse() }

            AdminUsersResponse(users = users, total = total, page = page, pageSize = pageSize)
        }

    fun updateRole(
        userId: UUID,
        role: Role,
    ): AdminUserResponse? =
        transaction {
            val updated =
                Users.update({ Users.id eq userId }) {
                    it[Users.role] = role
                    it[Users.updatedAt] = LocalDateTime.now()
                }
            if (updated == 0) {
                null
            } else {
                Users.select { Users.id eq userId }.single().toAdminResponse()
            }
        }

    private fun ResultRow.toAdminResponse() =
        AdminUserResponse(
            id = this[Users.id].toString(),
            email = this[Users.email],
            username = this[Users.username],
            role = this[Users.role].name,
            isVerified = this[Users.isVerified],
            createdAt = this[Users.createdAt].toString(),
        )
}
