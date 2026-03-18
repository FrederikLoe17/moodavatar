package com.moodavatar.auth.utils

import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.util.UUID

object HashUtils {
    /**
     * Erstellt einen BCrypt-Hash eines Passworts (cost factor 12)
     */
    fun hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt(12))

    /**
     * Prüft ein Klartext-Passwort gegen einen BCrypt-Hash
     */
    fun verifyPassword(
        password: String,
        hash: String,
    ): Boolean = runCatching { BCrypt.checkpw(password, hash) }.getOrDefault(false)

    /**
     * Validiert Passwort-Stärke:
     * min. 8 Zeichen, mind. 1 Zahl, mind. 1 Großbuchstabe
     */
    fun isStrongPassword(password: String): Boolean =
        password.length >= 8 &&
            password.any { it.isDigit() } &&
            password.any { it.isUpperCase() }

    /**
     * Generiert einen sicheren zufälligen Token (UUID-basiert)
     */
    fun generateSecureToken(): String = UUID.randomUUID().toString()

    /**
     * Erstellt einen SHA-256 Hash (z.B. für Token-Fingerprints)
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
