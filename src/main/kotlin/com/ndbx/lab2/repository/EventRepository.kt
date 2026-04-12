package com.ndbx.lab2.repository

import com.ndbx.lab2.document.EventDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface EventRepository : MongoRepository<EventDocument, String> {
    fun existsByTitle(title: String): Boolean
}
