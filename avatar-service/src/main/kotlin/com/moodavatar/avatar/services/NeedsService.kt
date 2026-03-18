package com.moodavatar.avatar.services

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.bson.Document
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

@Serializable
data class NeedsResponse(
    val mood: Int,
    val energy: Int,
    val social: Int,
    val activity: Int,
)

class NeedsService(
    private val db: MongoDatabase,
) {
    private val col = db.getCollection("avatar_needs")

    suspend fun getNeeds(userId: String): NeedsResponse =
        withContext(Dispatchers.IO) {
            val doc = col.find(Document("_id", userId)).firstOrNull()
            compute(doc)
        }

    suspend fun onMoodCheckin(userId: String) =
        withContext(Dispatchers.IO) {
            val now = Instant.now().toString()
            col.updateOne(
                Document("_id", userId),
                Document("\$set", Document("lastMoodCheckin", now).append("lastActivity", now)),
                UpdateOptions().upsert(true),
            )
        }

    suspend fun onSocialEvent(userId: String) =
        withContext(Dispatchers.IO) {
            val now = Instant.now().toString()
            col.updateOne(
                Document("_id", userId),
                Document("\$set", Document("lastSocialEvent", now).append("lastActivity", now)),
                UpdateOptions().upsert(true),
            )
        }

    private fun compute(doc: Document?): NeedsResponse {
        val now = Instant.now()

        fun decay(
            isoTs: String?,
            decayPerHour: Double,
        ): Int {
            if (isoTs == null) return 30
            val hours = ChronoUnit.MINUTES.between(Instant.parse(isoTs), now).toDouble() / 60.0
            return max(0, min(100, (100.0 - hours * decayPerHour).toInt()))
        }

        val lastMood = doc?.getString("lastMoodCheckin")
        val lastSocial = doc?.getString("lastSocialEvent")
        val lastActivity = doc?.getString("lastActivity")

        val mood = decay(lastMood, 5.0)
        val energy = decay(lastMood, 2.08)
        val social = decay(lastSocial, 2.78)
        val activity = decay(lastActivity, 3.57)

        return NeedsResponse(mood, energy, social, activity)
    }
}
