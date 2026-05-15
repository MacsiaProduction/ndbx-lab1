package com.ndbx.lab2.repository

import com.ndbx.lab2.document.UserDocument
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<UserDocument, String> {
    fun existsByUsername(username: String): Boolean
    fun findByUsername(username: String): UserDocument?
}
