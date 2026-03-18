package com.moodavatar.avatars.services

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.UpdateOptions
import com.moodavatar.avatars.dto.*
import com.moodavatar.avatars.models.emotionToMoodConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import java.time.Instant

class AvatarService(private val db: MongoDatabase) {
    private val col = db.getCollection("avatars")

    suspend fun setMood(userId: String, emotion: String, intensity: Int, note: String?): AvatarResponse =
        withContext(Dispatchers.IO) {
            val now = Instant.now().toString()
            val (primaryColor, expression, aura) = emotionToMoodConfig(emotion)

            val existing       = col.find(Document("_id", userId)).firstOrNull()
            val existingConfig = existing?.get("config") as? Document

            fun str(key: String, default: String) = existingConfig?.getString(key) ?: default

            @Suppress("UNCHECKED_CAST")
            fun list(key: String): List<String> = (existingConfig?.get(key) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val moodDoc = Document()
                .append("emotion",   emotion.uppercase())
                .append("intensity", intensity)
                .append("note",      note)
                .append("setAt",     now)

            val configDoc = Document()
                .append("primaryColor",   primaryColor)
                .append("expression",     expression)
                .append("aura",           aura)
                .append("secondaryColor", str("secondaryColor", "#94a3b8"))
                .append("hairStyle",      str("hairStyle",      "short"))
                .append("accessories",    list("accessories"))
                .append("skinColor",      str("skinColor",      "#f0c98b"))
                .append("clothesColor",   str("clothesColor",   "#3b82f6"))
                .append("roomWallColor",  str("roomWallColor",  "#1e293b"))
                .append("roomFloorColor", str("roomFloorColor", "#0f172a"))
                .append("roomItems",      list("roomItems"))

            val filter = Document("_id", userId)
            val update = Document()
                .append("\$set", Document()
                    .append("currentMood", moodDoc)
                    .append("config",      configDoc)
                    .append("updatedAt",   now))
                .append("\$push", Document("history",
                    Document("\$each", listOf(moodDoc)).append("\$slice", -365)))

            col.updateOne(filter, update, UpdateOptions().upsert(true))
            getAvatar(userId)!!
        }

    suspend fun updateConfig(userId: String, req: UpdateConfigRequest): AvatarResponse =
        withContext(Dispatchers.IO) {
            val now       = Instant.now().toString()
            val filter    = Document("_id", userId)
            val setFields = Document().append("updatedAt", now)

            req.secondaryColor?.let { setFields.append("config.secondaryColor", it) }
            req.hairStyle?.let      { setFields.append("config.hairStyle",      it) }
            req.accessories?.let    { setFields.append("config.accessories",    it) }
            req.skinColor?.let      { setFields.append("config.skinColor",      it) }
            req.clothesColor?.let   { setFields.append("config.clothesColor",   it) }
            req.roomWallColor?.let  { setFields.append("config.roomWallColor",  it) }
            req.roomFloorColor?.let { setFields.append("config.roomFloorColor", it) }
            req.roomItems?.let      { setFields.append("config.roomItems",      it) }

            col.updateOne(filter, Document("\$set", setFields), UpdateOptions().upsert(true))
            getAvatar(userId) ?: run {
                val defaultConfig = Document()
                    .append("primaryColor",   "#64748b")
                    .append("expression",     "neutral")
                    .append("aura",           "gray")
                    .append("secondaryColor", req.secondaryColor  ?: "#94a3b8")
                    .append("hairStyle",      req.hairStyle       ?: "short")
                    .append("accessories",    req.accessories     ?: emptyList<String>())
                    .append("skinColor",      req.skinColor       ?: "#f0c98b")
                    .append("clothesColor",   req.clothesColor    ?: "#3b82f6")
                    .append("roomWallColor",  req.roomWallColor   ?: "#1e293b")
                    .append("roomFloorColor", req.roomFloorColor  ?: "#0f172a")
                    .append("roomItems",      req.roomItems       ?: emptyList<String>())
                col.updateOne(
                    filter,
                    Document("\$set", Document("config", defaultConfig).append("updatedAt", now)),
                    UpdateOptions().upsert(true)
                )
                getAvatar(userId)!!
            }
        }

    suspend fun getAvatar(userId: String): AvatarResponse? =
        withContext(Dispatchers.IO) {
            col.find(Document("_id", userId)).firstOrNull()?.toAvatarResponse()
        }

    suspend fun getMoodHistory(userId: String, limit: Int = 20): List<MoodEntryResponse> =
        withContext(Dispatchers.IO) {
            val doc = col.find(Document("_id", userId)).firstOrNull() ?: return@withContext emptyList()
            @Suppress("UNCHECKED_CAST")
            val history = doc["history"] as? List<Document> ?: emptyList()
            history.takeLast(limit).reversed().map { it.toMoodEntry() }
        }

    suspend fun getCalendarData(userId: String, days: Int = 90): List<CalendarDayResponse> =
        withContext(Dispatchers.IO) {
            val doc = col.find(Document("_id", userId)).firstOrNull() ?: return@withContext emptyList()
            @Suppress("UNCHECKED_CAST")
            val history = doc["history"] as? List<Document> ?: emptyList()
            val cutoff  = Instant.now().minusSeconds(days.toLong() * 86400)

            history
                .filter { entry ->
                    val setAt = entry.getString("setAt") ?: return@filter false
                    Instant.parse(setAt).isAfter(cutoff)
                }
                .groupBy { entry ->
                    entry.getString("setAt")?.substring(0, 10) ?: ""
                }
                .filterKeys { it.isNotEmpty() }
                .map { (date, entries) ->
                    val dominant = entries
                        .map { it.getString("emotion") ?: "NEUTRAL" }
                        .groupingBy { it }.eachCount()
                        .maxByOrNull { it.value }?.key
                    val avg = entries.map { it.getInteger("intensity", 5).toDouble() }.average()
                    CalendarDayResponse(date = date, count = entries.size, avgIntensity = avg, dominantEmotion = dominant)
                }
                .sortedBy { it.date }
        }

    suspend fun getInsights(userId: String): InsightsResponse =
        withContext(Dispatchers.IO) {
            val doc = col.find(Document("_id", userId)).firstOrNull()
            @Suppress("UNCHECKED_CAST")
            val history = doc?.get("history") as? List<Document> ?: emptyList()

            val datesWithEntries = history
                .mapNotNull { it.getString("setAt")?.substring(0, 10) }
                .toSortedSet()

            var currentStreak = 0
            var checkDate     = java.time.LocalDate.now()
            while (datesWithEntries.contains(checkDate.toString())) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
            if (currentStreak == 0) {
                checkDate = java.time.LocalDate.now().minusDays(1)
                while (datesWithEntries.contains(checkDate.toString())) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                }
            }

            var longestStreak = 0
            var runningStreak = 0
            var prevDate: java.time.LocalDate? = null
            for (dateStr in datesWithEntries) {
                val date = java.time.LocalDate.parse(dateStr)
                runningStreak = if (prevDate != null && date == prevDate!!.plusDays(1)) runningStreak + 1 else 1
                if (runningStreak > longestStreak) longestStreak = runningStreak
                prevDate = date
            }

            val emotionCounts = history
                .groupBy { it.getString("emotion") ?: "NEUTRAL" }
                .map { (emotion, entries) -> emotion to entries.size }
                .sortedByDescending { it.second }
            val totalEntries = emotionCounts.sumOf { it.second }
            val emotionDistribution = emotionCounts.map { (emotion, count) ->
                EmotionDistribution(
                    emotion    = emotion,
                    count      = count,
                    percentage = if (totalEntries > 0) count.toDouble() / totalEntries * 100 else 0.0,
                )
            }

            val avgIntensity = if (history.isEmpty()) 0.0
            else history.map { it.getInteger("intensity", 5).toDouble() }.average()

            val dayNames   = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
            val byDayOfWeek = history.groupBy { entry ->
                val setAt = entry.getString("setAt") ?: return@groupBy -1
                java.time.LocalDate.parse(setAt.substring(0, 10)).dayOfWeek.value - 1
            }.filterKeys { it >= 0 }
            val weekdayPattern = (0..6).map { dayIdx ->
                val entries = byDayOfWeek[dayIdx] ?: emptyList()
                WeekdayStats(
                    day          = dayNames[dayIdx],
                    avgIntensity = if (entries.isEmpty()) 0.0 else entries.map { it.getInteger("intensity", 5).toDouble() }.average(),
                    count        = entries.size,
                )
            }

            InsightsResponse(
                totalEntries        = totalEntries,
                currentStreak       = currentStreak,
                longestStreak       = longestStreak,
                mostCommonEmotion   = emotionDistribution.firstOrNull()?.emotion,
                avgIntensity        = avgIntensity,
                emotionDistribution = emotionDistribution,
                weekdayPattern      = weekdayPattern,
            )
        }

    suspend fun getAdminStats(): MoodAdminStatsResponse =
        withContext(Dispatchers.IO) {
            val emotionDist = col.aggregate(listOf(
                Document("\$unwind", "\$history"),
                Document("\$group", Document("_id", "\$history.emotion").append("count", Document("\$sum", 1))),
                Document("\$sort",  Document("count", -1)),
            )).map { EmotionCount(emotion = it.getString("_id") ?: "UNKNOWN", count = it.getInteger("count", 0).toLong()) }
                .toList()

            val totalEntries = emotionDist.sumOf { it.count }
            val activeUsers  = col.countDocuments(Document("history.0", Document("\$exists", true)))
            val avgIntensity = col.aggregate(listOf(
                Document("\$unwind", "\$history"),
                Document("\$group",  Document("_id", null).append("avg", Document("\$avg", "\$history.intensity"))),
            )).firstOrNull()?.getDouble("avg") ?: 0.0

            val recentMoods = col.aggregate(listOf(
                Document("\$unwind", "\$history"),
                Document("\$sort",   Document("history.setAt", -1)),
                Document("\$limit",  20),
                Document("\$project", Document("_id", 1)
                    .append("emotion",   "\$history.emotion")
                    .append("intensity", "\$history.intensity")
                    .append("setAt",     "\$history.setAt")),
            )).map {
                RecentMoodEntry(
                    userId    = it.getString("_id") ?: "",
                    emotion   = it.getString("emotion")   ?: "NEUTRAL",
                    intensity = it.getInteger("intensity", 5),
                    setAt     = it.getString("setAt")     ?: "",
                )
            }.toList()

            MoodAdminStatsResponse(
                emotionDistribution = emotionDist,
                totalMoodEntries    = totalEntries,
                activeUsers         = activeUsers,
                averageIntensity    = avgIntensity,
                recentMoods         = recentMoods,
            )
        }

    private fun Document.toAvatarResponse(): AvatarResponse {
        val mood   = get("currentMood") as? Document
        val config = get("config") as? Document

        fun str(key: String, default: String) = config?.getString(key) ?: default

        @Suppress("UNCHECKED_CAST")
        fun list(key: String): List<String> = (config?.get(key) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return AvatarResponse(
            userId      = getString("_id"),
            currentMood = mood?.toMoodEntry(),
            config = AvatarConfigResponse(
                primaryColor   = str("primaryColor",   "#64748b"),
                expression     = str("expression",     "neutral"),
                aura           = str("aura",           "gray"),
                secondaryColor = str("secondaryColor", "#94a3b8"),
                hairStyle      = str("hairStyle",      "short"),
                accessories    = list("accessories"),
                skinColor      = str("skinColor",      "#f0c98b"),
                clothesColor   = str("clothesColor",   "#3b82f6"),
                roomWallColor  = str("roomWallColor",  "#1e293b"),
                roomFloorColor = str("roomFloorColor", "#0f172a"),
                roomItems      = list("roomItems"),
            ),
            updatedAt = getString("updatedAt") ?: "",
        )
    }

    private fun Document.toMoodEntry() = MoodEntryResponse(
        emotion   = getString("emotion"),
        intensity = getInteger("intensity"),
        note      = getString("note"),
        setAt     = getString("setAt"),
    )
}
