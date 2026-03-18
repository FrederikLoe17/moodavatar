package com.moodavatar.auth

import com.moodavatar.auth.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureDatabase()
    configureSecurity()
    configureSerialization()
    configureRouting()
}
