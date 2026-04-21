package com.ndbx.lab2.repository

import com.ndbx.lab2.document.EventDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface EventRepository : MongoRepository<EventDocument, ObjectId>
