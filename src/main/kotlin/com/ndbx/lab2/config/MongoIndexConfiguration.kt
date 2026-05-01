package com.ndbx.lab2.config

import com.ndbx.lab2.document.EventDocument
import com.ndbx.lab2.document.UserDocument
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index

@Configuration
class MongoIndexConfiguration(
    private val mongoTemplate: MongoTemplate,
) {

    @EventListener(ApplicationReadyEvent::class)
    fun ensureIndexes() {
        mongoTemplate.indexOps(UserDocument::class.java)
            .ensureIndex(Index().on("username", Sort.Direction.ASC).unique())
        mongoTemplate.indexOps(UserDocument::class.java)
            .ensureIndex(Index().on("full_name", Sort.Direction.ASC))
        mongoTemplate.indexOps(EventDocument::class.java).apply {
            ensureIndex(Index().on("title", Sort.Direction.ASC))
            ensureIndex(Index().on("created_by", Sort.Direction.ASC))
            ensureIndex(
                Index()
                    .on("title", Sort.Direction.ASC)
                    .on("created_by", Sort.Direction.ASC),
            )
            ensureIndex(Index().on("category", Sort.Direction.ASC))
            ensureIndex(Index().on("price", Sort.Direction.ASC))
            ensureIndex(Index().on("location.city", Sort.Direction.ASC))
            ensureIndex(Index().on("started_at", Sort.Direction.ASC))
        }
    }
}
