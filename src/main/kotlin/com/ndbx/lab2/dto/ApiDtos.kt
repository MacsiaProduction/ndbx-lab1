package com.ndbx.lab2.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class MessageResponse(
    @JsonProperty("message") val message: String,
)

data class SignUpRequest(
    @JsonProperty("full_name") val fullName: String?,
    @JsonProperty("username") val username: String?,
    @JsonProperty("password") val password: String?,
)

data class LoginRequest(
    @JsonProperty("username") val username: String?,
    @JsonProperty("password") val password: String?,
)

data class CreateEventRequest(
    @JsonProperty("title") val title: String?,
    @JsonProperty("address") val address: String?,
    @JsonProperty("started_at") val startedAt: String?,
    @JsonProperty("finished_at") val finishedAt: String?,
    @JsonProperty("description") val description: String? = null,
)

data class CreateEventResponse(
    @JsonProperty("id") val id: String,
)

data class EventLocationJson(
    @JsonProperty("address") val address: String,
)

data class EventListItemJson(
    @JsonProperty("id") val id: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("description") val description: String?,
    @JsonProperty("location") val location: EventLocationJson,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("created_by") val createdBy: String,
    @JsonProperty("started_at") val startedAt: String,
    @JsonProperty("finished_at") val finishedAt: String,
)

data class EventListResponse(
    @JsonProperty("events") val events: List<EventListItemJson>,
    @JsonProperty("count") val count: Int,
)
