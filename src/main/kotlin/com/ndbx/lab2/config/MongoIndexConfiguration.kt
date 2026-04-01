package com.ndbx.lab2.config

import com.ndbx.lab2.document.EventDocument
import com.ndbx.lab2.document.UserDocument
import org.bson.Document
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition
import org.springframework.data.mongodb.core.index.Index

@Configuration
class MongoIndexConfiguration(
    private val mongoTemplate: MongoTemplate,
) {

    @EventListener(ApplicationReadyEvent::class)
    fun ensureIndexes() {
        mongoTemplate.indexOps(UserDocument::class.java)
            .ensureIndex(Index().on("username", Sort.Direction.ASC).unique())
        mongoTemplate.indexOps(EventDocument::class.java).apply {
            ensureIndex(Index().on("title", Sort.Direction.ASC).unique())
            ensureIndex(
                CompoundIndexDefinition(Document("title", 1).append("created_by", 1)),
            )
            ensureIndex(Index().on("created_by", Sort.Direction.ASC))
        }
    }
}
