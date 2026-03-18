package com.moodavatar.users.services

import com.moodavatar.users.dto.UserAdminStatsResponse
import com.moodavatar.users.models.FriendRequests
import com.moodavatar.users.models.FriendshipStatus
import com.moodavatar.users.models.Profiles
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class UserAdminService {
    fun getStats(): UserAdminStatsResponse =
        transaction {
            UserAdminStatsResponse(
                totalProfiles   = Profiles.selectAll().count(),
                totalFriendships = FriendRequests
                    .select { FriendRequests.status eq FriendshipStatus.ACCEPTED }
                    .count(),
                pendingRequests = FriendRequests
                    .select { FriendRequests.status eq FriendshipStatus.PENDING }
                    .count(),
            )
        }
}
