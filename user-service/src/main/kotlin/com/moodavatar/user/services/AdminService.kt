package com.moodavatar.user.services

import com.moodavatar.user.models.FriendRequests
import com.moodavatar.user.models.FriendshipStatus
import com.moodavatar.user.models.Profiles
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class UserAdminStatsResponse(
    val totalProfiles: Long,
    val totalFriendships: Long,
    val pendingRequests: Long,
)

class AdminService {
    fun getStats(): UserAdminStatsResponse =
        transaction {
            UserAdminStatsResponse(
                totalProfiles = Profiles.selectAll().count(),
                totalFriendships =
                    FriendRequests
                        .select {
                            FriendRequests.status eq FriendshipStatus.ACCEPTED
                        }.count(),
                pendingRequests =
                    FriendRequests
                        .select {
                            FriendRequests.status eq FriendshipStatus.PENDING
                        }.count(),
            )
        }
}
