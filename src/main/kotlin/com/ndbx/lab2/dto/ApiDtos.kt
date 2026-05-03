package com.ndbx.lab2.dto

import com.fasterxml.jackson.annotation.JsonInclude
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

data class UpdateEventRequest(
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("price") val price: Int? = null,
    @JsonProperty("city") val city: String? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventLocationJson(
    @JsonProperty("address") val address: String,
    @JsonProperty("city") val city: String? = null,
)

data class EventReactionsJson(
    @JsonProperty("likes") val likes: Int,
    @JsonProperty("dislikes") val dislikes: Int,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventListItemJson(
    @JsonProperty("id") val id: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("price") val price: Int? = null,
    @JsonProperty("description") val description: String?,
    @JsonProperty("location") val location: EventLocationJson,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("created_by") val createdBy: String,
    @JsonProperty("started_at") val startedAt: String,
    @JsonProperty("finished_at") val finishedAt: String,
    @JsonProperty("reactions") val reactions: EventReactionsJson? = null,
)

data class EventListResponse(
    @JsonProperty("events") val events: List<EventListItemJson>,
    @JsonProperty("count") val count: Int,
)

data class UserJson(
    @JsonProperty("id") val id: String,
    @JsonProperty("full_name") val fullName: String,
    @JsonProperty("username") val username: String,
)

data class UserListResponse(
    @JsonProperty("users") val users: List<UserJson>,
    @JsonProperty("count") val count: Int,
)
