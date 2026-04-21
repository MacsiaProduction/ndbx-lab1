package com.ndbx.lab2.service

import com.ndbx.lab2.document.EventDocument
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service

data class EventUpdateCommand(
    val category: String? = null,
    val price: Int? = null,
    val city: String? = null,
)

@Service
class EventCommandService(
    private val mongoTemplate: MongoTemplate,
) {
    fun updateEvent(eventId: String, organizerId: String, command: EventUpdateCommand) {
        var hasChanges = false
        val update = Update()
        command.category?.let {
            update.set("category", it)
            hasChanges = true
        }
        command.price?.let {
            update.set("price", it)
            hasChanges = true
        }
        command.city?.let {
            if (it.isEmpty()) {
                update.unset("location.city")
            } else {
                update.set("location.city", it)
            }
            hasChanges = true
        }
        if (!hasChanges) return
        mongoTemplate.updateFirst(
            Query.query(
                Criteria.where("_id").`is`(eventId)
                    .and("created_by").`is`(organizerId),
            ),
            update,
            EventDocument::class.java,
        )
    }
}
