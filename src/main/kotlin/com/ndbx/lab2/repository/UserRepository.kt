package com.ndbx.lab2.repository

import com.ndbx.lab2.document.UserDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<UserDocument, ObjectId> {
    fun existsByUsername(username: String): Boolean
    fun findByUsername(username: String): UserDocument?
}
