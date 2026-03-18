package com.moodavatar.avatar

import com.moodavatar.avatar.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain
        .main(args)

fun Application.module() {
    val db = configureDatabase()
    configureSecurity()
    configureSerialization()
    configureRouting(db)
}
