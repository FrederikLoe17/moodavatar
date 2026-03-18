package com.moodavatar.avatar.models

// Alle erlaubten Emotionen
enum class Emotion {
    HAPPY,
    SAD,
    ANGRY,
    NEUTRAL,
    EXCITED,
    TIRED,
    ANXIOUS,
    CONTENT,
}

// Visuelle Konfiguration: Mood-driven + User-Personalisierung
data class AvatarConfig(
    // Mood-driven (überschrieben bei jedem setMood)
    val primaryColor: String,
    val expression: String,
    val aura: String,
    // Charakter-Personalisierung (wird bei setMood beibehalten)
    val secondaryColor: String = "#94a3b8",
    val hairStyle: String = "short",
    val accessories: List<String> = emptyList(),
    val skinColor: String = "#f0c98b",
    val clothesColor: String = "#3b82f6",
    // Raum-Personalisierung
    val roomWallColor: String = "#1e293b",
    val roomFloorColor: String = "#0f172a",
    val roomItems: List<String> = emptyList(),
)

fun emotionToMoodConfig(emotion: String): Triple<String, String, String> =
    when (emotion.uppercase()) {
        "HAPPY" -> Triple("#10b981", "smile", "golden")
        "SAD" -> Triple("#3b82f6", "frown", "blue")
        "ANGRY" -> Triple("#ef4444", "angry", "red")
        "EXCITED" -> Triple("#f59e0b", "wide-eyes", "yellow")
        "TIRED" -> Triple("#8b5cf6", "half-eyes", "purple")
        "ANXIOUS" -> Triple("#f97316", "worried", "orange")
        "CONTENT" -> Triple("#06b6d4", "gentle-smile", "teal")
        else -> Triple("#64748b", "neutral", "gray")
    }
