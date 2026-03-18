package com.moodavatar.avatar.services

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.UpdateOptions
import com.moodavatar.avatar.dto.*
import com.moodavatar.avatar.models.emotionToMoodConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import java.time.Instant

class AvatarService(
    private val db: MongoDatabase,
) {
    private val col = db.getCollection("avatars")

    // ── Stimmung setzen ───────────────────────────────────────────────────────
    suspend fun setMood(
        userId: String,
        emotion: String,
        intensity: Int,
        note: String?,
    ): AvatarResponse =
        withContext(Dispatchers.IO) {
            val now = Instant.now().toString()
            val (primaryColor, expression, aura) = emotionToMoodConfig(emotion)

            // Bestehende User-Personalisierung beibehalten
            val existing = col.find(Document("_id", userId)).firstOrNull()
            val existingConfig = existing?.get("config") as? Document

            fun str(
                key: String,
                default: String,
            ) = existingConfig?.getString(key) ?: default

            @Suppress("UNCHECKED_CAST")
            fun list(key: String): List<String> = (existingConfig?.get(key) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val moodDoc =
                Document()
                    .append("emotion", emotion.uppercase())
                    .append("intensity", intensity)
                    .append("note", note)
                    .append("setAt", now)

            val configDoc =
                Document()
                    .append("primaryColor", primaryColor)
                    .append("expression", expression)
                    .append("aura", aura)
                    .append("secondaryColor", str("secondaryColor", "#94a3b8"))
                    .append("hairStyle", str("hairStyle", "short"))
                    .append("accessories", list("accessories"))
                    .append("skinColor", str("skinColor", "#f0c98b"))
                    .append("clothesColor", str("clothesColor", "#3b82f6"))
                    .append("roomWallColor", str("roomWallColor", "#1e293b"))
                    .append("roomFloorColor", str("roomFloorColor", "#0f172a"))
                    .append("roomItems", list("roomItems"))

            val filter = Document("_id", userId)
            val update =
                Document()
                    .append(
                        "\$set",
                        Document()
                            .append("currentMood", moodDoc)
                            .append("config", configDoc)
                            .append("updatedAt", now),
                    ).append(
                        "\$push",
                        Document(
                            "history",
                            Document("\$each", listOf(moodDoc)).append("\$slice", -50),
                        ),
                    )

            col.updateOne(filter, update, UpdateOptions().upsert(true))
            getAvatar(userId)!!
        }

    // ── Avatar-Konfiguration aktualisieren ────────────────────────────────────
    suspend fun updateConfig(
        userId: String,
        req: UpdateConfigRequest,
    ): AvatarResponse =
        withContext(Dispatchers.IO) {
            val now = Instant.now().toString()
            val filter = Document("_id", userId)

            val setFields = Document().append("updatedAt", now)
            req.secondaryColor?.let { setFields.append("config.secondaryColor", it) }
            req.hairStyle?.let { setFields.append("config.hairStyle", it) }
            req.accessories?.let { setFields.append("config.accessories", it) }
            req.skinColor?.let { setFields.append("config.skinColor", it) }
            req.clothesColor?.let { setFields.append("config.clothesColor", it) }
            req.roomWallColor?.let { setFields.append("config.roomWallColor", it) }
            req.roomFloorColor?.let { setFields.append("config.roomFloorColor", it) }
            req.roomItems?.let { setFields.append("config.roomItems", it) }

            col.updateOne(filter, Document("\$set", setFields), UpdateOptions().upsert(true))
            getAvatar(userId) ?: run {
                val defaultConfig =
                    Document()
                        .append("primaryColor", "#64748b")
                        .append("expression", "neutral")
                        .append("aura", "gray")
                        .append("secondaryColor", req.secondaryColor ?: "#94a3b8")
                        .append("hairStyle", req.hairStyle ?: "short")
                        .append("accessories", req.accessories ?: emptyList<String>())
                        .append("skinColor", req.skinColor ?: "#f0c98b")
                        .append("clothesColor", req.clothesColor ?: "#3b82f6")
                        .append("roomWallColor", req.roomWallColor ?: "#1e293b")
                        .append("roomFloorColor", req.roomFloorColor ?: "#0f172a")
                        .append("roomItems", req.roomItems ?: emptyList<String>())
                col.updateOne(
                    filter,
                    Document("\$set", Document("config", defaultConfig).append("updatedAt", now)),
                    UpdateOptions().upsert(true),
                )
                getAvatar(userId)!!
            }
        }

    // ── Avatar abrufen ────────────────────────────────────────────────────────
    suspend fun getAvatar(userId: String): AvatarResponse? =
        withContext(Dispatchers.IO) {
            col.find(Document("_id", userId)).firstOrNull()?.toAvatarResponse()
        }

    // ── Mood-Historie ─────────────────────────────────────────────────────────
    suspend fun getMoodHistory(
        userId: String,
        limit: Int = 20,
    ): List<MoodEntryResponse> =
        withContext(Dispatchers.IO) {
            val doc =
                col.find(Document("_id", userId)).firstOrNull()
                    ?: return@withContext emptyList()

            @Suppress("UNCHECKED_CAST")
            val history = doc["history"] as? List<Document> ?: emptyList()
            history.takeLast(limit).reversed().map { it.toMoodEntry() }
        }

    // ── Admin: Stimmungsstatistiken ───────────────────────────────────────────
    suspend fun getAdminStats(): MoodAdminStatsResponse =
        withContext(Dispatchers.IO) {
            // 1. Emotion-Verteilung (alle history-Einträge aller Nutzer)
            val emotionDist =
                col
                    .aggregate(
                        listOf(
                            Document("\$unwind", "\$history"),
                            Document("\$group", Document("_id", "\$history.emotion").append("count", Document("\$sum", 1))),
                            Document("\$sort", Document("count", -1)),
                        ),
                    ).map { EmotionCount(emotion = it.getString("_id") ?: "UNKNOWN", count = it.getInteger("count", 0).toLong()) }
                    .toList()

            val totalEntries = emotionDist.sumOf { it.count }

            // 2. Aktive Nutzer (haben mindestens einen History-Eintrag)
            val activeUsers = col.countDocuments(Document("history.0", Document("\$exists", true)))

            // 3. Durchschnittliche Intensität
            val avgIntensity =
                col
                    .aggregate(
                        listOf(
                            Document("\$unwind", "\$history"),
                            Document("\$group", Document("_id", null).append("avg", Document("\$avg", "\$history.intensity"))),
                        ),
                    ).firstOrNull()
                    ?.getDouble("avg") ?: 0.0

            // 4. Neueste Stimmungen (letzte 20 Einträge über alle Nutzer)
            val recentMoods =
                col
                    .aggregate(
                        listOf(
                            Document("\$unwind", "\$history"),
                            Document("\$sort", Document("history.setAt", -1)),
                            Document("\$limit", 20),
                            Document(
                                "\$project",
                                Document("_id", 1)
                                    .append("emotion", "\$history.emotion")
                                    .append("intensity", "\$history.intensity")
                                    .append("setAt", "\$history.setAt"),
                            ),
                        ),
                    ).map {
                        RecentMoodEntry(
                            userId = it.getString("_id") ?: "",
                            emotion = it.getString("emotion") ?: "NEUTRAL",
                            intensity = it.getInteger("intensity", 5),
                            setAt = it.getString("setAt") ?: "",
                        )
                    }.toList()

            MoodAdminStatsResponse(
                emotionDistribution = emotionDist,
                totalMoodEntries = totalEntries,
                activeUsers = activeUsers,
                averageIntensity = avgIntensity,
                recentMoods = recentMoods,
            )
        }

    // ── Mapping ───────────────────────────────────────────────────────────────
    private fun Document.toAvatarResponse(): AvatarResponse {
        val mood = get("currentMood") as? Document
        val config = get("config") as? Document

        fun str(
            key: String,
            default: String,
        ) = config?.getString(key) ?: default

        @Suppress("UNCHECKED_CAST")
        fun list(key: String): List<String> = (config?.get(key) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return AvatarResponse(
            userId = getString("_id"),
            currentMood = mood?.toMoodEntry(),
            config =
                AvatarConfigResponse(
                    primaryColor = str("primaryColor", "#64748b"),
                    expression = str("expression", "neutral"),
                    aura = str("aura", "gray"),
                    secondaryColor = str("secondaryColor", "#94a3b8"),
                    hairStyle = str("hairStyle", "short"),
                    accessories = list("accessories"),
                    skinColor = str("skinColor", "#f0c98b"),
                    clothesColor = str("clothesColor", "#3b82f6"),
                    roomWallColor = str("roomWallColor", "#1e293b"),
                    roomFloorColor = str("roomFloorColor", "#0f172a"),
                    roomItems = list("roomItems"),
                ),
            updatedAt = getString("updatedAt") ?: "",
        )
    }

    private fun Document.toMoodEntry() =
        MoodEntryResponse(
            emotion = getString("emotion"),
            intensity = getInteger("intensity"),
            note = getString("note"),
            setAt = getString("setAt"),
        )
}
