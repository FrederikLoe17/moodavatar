package com.moodavatar.notification.services

import com.rabbitmq.client.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

private val log = LoggerFactory.getLogger("RabbitMQConsumer")
private val json = Json { ignoreUnknownKeys = true }

const val EXCHANGE = "moodavatar.events"
const val QUEUE_NAME = "notification-service"

class RabbitMQConsumer(
    host: String,
    port: Int,
    user: String,
    password: String,
    private val notificationService: NotificationService,
) {
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

    fun start() {
        // Connect in background so service starts even if RabbitMQ is temporarily unavailable
        Executors.newSingleThreadExecutor().submit {
            repeat(10) { attempt ->
                try {
                    connect()
                    return@submit
                } catch (e: Exception) {
                    log.warn("RabbitMQ connect attempt ${attempt + 1} failed: ${e.message}. Retrying in 5s…")
                    Thread.sleep(5_000)
                }
            }
            log.error("Could not connect to RabbitMQ after 10 attempts")
        }
    }

    private fun connect() {
        connection = factory.newConnection()
        channel =
            connection!!.createChannel().also { ch ->
                ch.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, true)
                ch.queueDeclare(QUEUE_NAME, true, false, false, null)
                ch.queueBind(QUEUE_NAME, EXCHANGE, "friend.request.*")

                ch.basicConsume(
                    QUEUE_NAME,
                    true,
                    object : DefaultConsumer(ch) {
                        override fun handleDelivery(
                            consumerTag: String,
                            envelope: Envelope,
                            properties: AMQP.BasicProperties,
                            body: ByteArray,
                        ) {
                            handleEvent(envelope.routingKey, String(body))
                        }
                    },
                )
            }
        log.info("RabbitMQ consumer ready — listening on '$QUEUE_NAME'")
    }

    private fun handleEvent(
        routingKey: String,
        body: String,
    ) {
        try {
            val obj = json.parseToJsonElement(body).jsonObject
            when (routingKey) {
                "friend.request.sent" -> {
                    val receiverId = obj["receiverId"]?.jsonPrimitive?.content ?: return
                    val senderId = obj["senderId"]?.jsonPrimitive?.content ?: return
                    val senderUsername = obj["senderUsername"]?.jsonPrimitive?.content ?: return
                    notificationService.create(
                        userId = receiverId,
                        type = "FRIEND_REQUEST",
                        fromUserId = senderId,
                        fromUsername = senderUsername,
                    )
                    log.info("Notification: FRIEND_REQUEST for $receiverId from $senderUsername")
                }
                "friend.request.accepted" -> {
                    val originalSenderId = obj["senderId"]?.jsonPrimitive?.content ?: return
                    val acceptorId = obj["acceptorId"]?.jsonPrimitive?.content ?: return
                    val acceptorUsername = obj["acceptorUsername"]?.jsonPrimitive?.content ?: return
                    notificationService.create(
                        userId = originalSenderId,
                        type = "FRIEND_ACCEPTED",
                        fromUserId = acceptorId,
                        fromUsername = acceptorUsername,
                    )
                    log.info("Notification: FRIEND_ACCEPTED for $originalSenderId from $acceptorUsername")
                }
            }
        } catch (e: Exception) {
            log.error("Error handling event '$routingKey': ${e.message}")
        }
    }

    fun close() {
        channel?.close()
        connection?.close()
    }
}
