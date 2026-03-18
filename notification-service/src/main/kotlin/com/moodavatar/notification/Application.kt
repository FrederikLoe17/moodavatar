package com.moodavatar.notification

import com.moodavatar.notification.db.configureDatabase
import com.moodavatar.notification.plugins.configureRouting
import com.moodavatar.notification.plugins.configureSecurity
import com.moodavatar.notification.services.NotificationService
import com.moodavatar.notification.services.RabbitMQConsumer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(CallLogging) { level = Level.INFO }

    configureDatabase()
    configureSecurity()

    val notificationService = NotificationService()

    val rmqHost = environment.config.property("rabbitmq.host").getString()
    val rmqPort =
        environment.config
            .property("rabbitmq.port")
            .getString()
            .toInt()
    val rmqUser = environment.config.property("rabbitmq.user").getString()
    val rmqPassword = environment.config.property("rabbitmq.password").getString()

    val consumer = RabbitMQConsumer(rmqHost, rmqPort, rmqUser, rmqPassword, notificationService)
    consumer.start()

    environment.monitor.subscribe(ApplicationStopped) { consumer.close() }

    configureRouting(notificationService)
}
