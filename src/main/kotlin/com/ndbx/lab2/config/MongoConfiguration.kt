package com.ndbx.lab2.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories("com.ndbx.lab2.repository")
class MongoConfiguration(
    private val appMongoProperties: AppMongoProperties,
) : AbstractMongoClientConfiguration() {

    override fun getDatabaseName(): String = appMongoProperties.database

    override fun mongoClient(): MongoClient = MongoClients.create(appMongoProperties.connectionUri())
}
