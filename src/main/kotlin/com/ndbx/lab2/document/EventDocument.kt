package com.ndbx.lab2.document

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

data class EventLocation(
    @Field("address") val address: String,
    @Field("city") val city: String? = null,
)

@Document(collection = "events")
data class EventDocument(
    @Id val id: String? = null,
    @Field("title") val title: String,
    @Field("description") val description: String?,
    @Field("location") val location: EventLocation,
    @Field("created_at") val createdAt: String,
    @Field("created_by") val createdBy: String,
    @Field("started_at") val startedAt: String,
    @Field("finished_at") val finishedAt: String,
    @Field("category") val category: String? = null,
    @Field("price") val price: Int? = null,
)
