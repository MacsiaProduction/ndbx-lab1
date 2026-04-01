package com.ndbx.lab2.document

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "users")
data class UserDocument(
    @Id val id: ObjectId? = null,
    @Field("full_name") val fullName: String,
    @Field("username") val username: String,
    @Field("password_hash") val passwordHash: String,
)
