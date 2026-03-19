package com.moodavatar

import com.moodavatar.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val mongoDb = configureDatabase()
    configureSecurity()
    configureSerialization()
    configureHTTP()
    configureWebSockets()
    configureRouting(mongoDb)
}
