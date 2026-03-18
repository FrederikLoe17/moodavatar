package com.moodavatar.user.services

import com.moodavatar.user.dto.*
import com.moodavatar.user.models.Profiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class ProfileService {
    fun createProfile(req: CreateProfileRequest): ProfileResponse =
        transaction {
            val id = UUID.fromString(req.id)
            val now = LocalDateTime.now()
            Profiles.insert {
                it[Profiles.id] = id
                it[username] = req.username
                it[displayName] = req.displayName
                it[bio] = null
                it[avatarUrl] = null
                it[createdAt] = now
                it[updatedAt] = now
            }
            ProfileResponse(id.toString(), req.username, req.displayName, null, null)
        }

    fun getProfile(id: UUID): ProfileResponse? =
        transaction {
            Profiles.select { Profiles.id eq id }.singleOrNull()?.toResponse()
        }

    fun getProfileByUsername(username: String): ProfileResponse? =
        transaction {
            Profiles.select { Profiles.username eq username }.singleOrNull()?.toResponse()
        }

    fun searchProfiles(query: String): List<ProfileResponse> =
        transaction {
            Profiles
                .select { Profiles.username like "%$query%" }
                .limit(20)
                .map { it.toResponse() }
        }

    fun updateProfile(
        id: UUID,
        req: UpdateProfileRequest,
    ): ProfileResponse? =
        transaction {
            val updated =
                Profiles.update({ Profiles.id eq id }) {
                    req.displayName?.let { v -> it[displayName] = v }
                    req.bio?.let { v -> it[bio] = v }
                    req.avatarUrl?.let { v -> it[avatarUrl] = v }
                    it[updatedAt] = LocalDateTime.now()
                }
            if (updated == 0) {
                null
            } else {
                Profiles.select { Profiles.id eq id }.single().toResponse()
            }
        }

    private fun ResultRow.toResponse() =
        ProfileResponse(
            id = this[Profiles.id].toString(),
            username = this[Profiles.username],
            displayName = this[Profiles.displayName],
            bio = this[Profiles.bio],
            avatarUrl = this[Profiles.avatarUrl],
        )
}
