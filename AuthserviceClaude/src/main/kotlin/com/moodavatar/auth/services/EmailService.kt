package com.moodavatar.auth.services

import io.ktor.server.config.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

class EmailService(
    cfg: ApplicationConfig,
) {
    private val host = cfg.property("smtp.host").getString()
    private val port = cfg.property("smtp.port").getString().toInt()
    private val user = cfg.property("smtp.user").getString()
    private val password = cfg.property("smtp.password").getString()
    private val baseUrl = cfg.property("app.baseUrl").getString()

    private val session: Session by lazy {
        val props =
            Properties().apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                put("mail.smtp.auth", if (user.isNotEmpty()) "true" else "false")
            }
        if (user.isNotEmpty()) {
            Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication() = PasswordAuthentication(user, password)
                },
            )
        } else {
            Session.getInstance(props)
        }
    }

    fun sendPasswordReset(
        to: String,
        token: String,
    ) {
        val resetLink = "$baseUrl/reset-password?token=$token"
        sendEmail(
            to = to,
            subject = "MoodAvatar – Passwort zurücksetzen",
            html =
                """
                <h2>Passwort zurücksetzen</h2>
                <p>Klicke auf den Link, um dein Passwort zurückzusetzen:</p>
                <a href="$resetLink" style="background:#6366f1;color:white;padding:12px 24px;border-radius:8px;text-decoration:none;">
                    Passwort zurücksetzen
                </a>
                <p style="color:#64748b;font-size:12px;margin-top:16px;">
                    Dieser Link ist 1 Stunde gültig. Falls du kein Reset angefordert hast, ignoriere diese E-Mail.
                </p>
                """.trimIndent(),
        )
    }

    fun sendEmailVerification(
        to: String,
        token: String,
    ) {
        val verifyLink = "$baseUrl/verify-email?token=$token"
        sendEmail(
            to = to,
            subject = "MoodAvatar – E-Mail bestätigen",
            html =
                """
                <h2>E-Mail bestätigen</h2>
                <p>Klicke auf den Link, um deine E-Mail-Adresse zu bestätigen:</p>
                <a href="$verifyLink" style="background:#10b981;color:white;padding:12px 24px;border-radius:8px;text-decoration:none;display:inline-block;margin:12px 0">
                    E-Mail bestätigen
                </a>
                <p style="color:#64748b;font-size:12px;margin-top:16px;">
                    Dieser Link ist 24 Stunden gültig. Falls du kein Konto erstellt hast, ignoriere diese E-Mail.
                </p>
                """.trimIndent(),
        )
    }

    private fun sendEmail(
        to: String,
        subject: String,
        html: String,
    ) {
        try {
            val msg =
                MimeMessage(session).apply {
                    setFrom(InternetAddress("noreply@moodavatar.app"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                    setSubject(subject, "UTF-8")
                    setContent(html, "text/html; charset=UTF-8")
                }
            Transport.send(msg)
        } catch (e: Exception) {
            // Log error but don't crash the app
            println("Email sending failed: ${e.message}")
        }
    }
}
