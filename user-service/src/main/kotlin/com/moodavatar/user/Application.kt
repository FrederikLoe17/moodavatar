package com.moodavatar.user

import com.moodavatar.user.plugins.*
import com.moodavatar.user.services.EventPublisher
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    configureDatabase()
    configureSecurity()
    configureSerialization()

    val rmqHost     = environment.config.propertyOrNull("rabbitmq.host")?.getString() ?: "rabbitmq"
    val rmqPort     = environment.config.propertyOrNull("rabbitmq.port")?.getString()?.toInt() ?: 5672
    val rmqUser     = environment.config.propertyOrNull("rabbitmq.user")?.getString() ?: "guest"
    val rmqPassword = environment.config.propertyOrNull("rabbitmq.password")?.getString() ?: "guest"

    val eventPublisher = EventPublisher(rmqHost, rmqPort, rmqUser, rmqPassword)
    environment.monitor.subscribe(ApplicationStopped) { eventPublisher.close() }

    configureRouting(eventPublisher)
}
