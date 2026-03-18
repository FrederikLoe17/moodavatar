package com.moodavatar.avatar.plugins

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.ktor.server.application.*

fun Application.configureDatabase(): MongoDatabase {
    val cfg  = environment.config
    val uri  = cfg.property("mongodb.uri").getString()
    val name = cfg.property("mongodb.db").getString()

    val client: MongoClient = MongoClients.create(uri)
    return client.getDatabase(name)
}
