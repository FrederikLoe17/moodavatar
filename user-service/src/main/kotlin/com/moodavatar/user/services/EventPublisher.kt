package com.moodavatar.user.services

import com.rabbitmq.client.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

private const val EXCHANGE = "moodavatar.events"

class EventPublisher(
    host: String,
    port: Int,
    user: String,
    password: String,
) {
    private val log = LoggerFactory.getLogger(EventPublisher::class.java)

    private val factory =
        ConnectionFactory().apply {
            this.host = host
            this.port = port
            this.username = user
            this.password = password
            isAutomaticRecoveryEnabled = true
            networkRecoveryInterval = 5_000
        }

    private var connection: Connection? = null
    private var channel: Channel? = null

    init {
        Executors.newSingleThreadExecutor().submit {
            repeat(10) { attempt ->
                try {
                    connection = factory.newConnection()
                    channel =
                        connection!!.createChannel().also {
                            it.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, true)
                        }
                    log.info("RabbitMQ publisher ready")
                    return@submit
                } catch (e: Exception) {
                    log.warn("RabbitMQ publisher connect attempt ${attempt + 1} failed: ${e.message}. Retrying in 5s…")
                    Thread.sleep(5_000)
                }
            }
        }
    }

    fun publish(
        routingKey: String,
        json: String,
    ) {
        try {
            channel?.basicPublish(
                EXCHANGE,
                routingKey,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                json.toByteArray(),
            )
        } catch (e: Exception) {
            log.warn("Failed to publish event '$routingKey': ${e.message}")
        }
    }

    fun close() {
        channel?.close()
        connection?.close()
    }
}
