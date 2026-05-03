package com.ndbx.lab2.support

import com.ndbx.lab2.document.EventDocument
import com.ndbx.lab2.document.UserDocument
import com.ndbx.lab2.dto.EventListItemJson
import com.ndbx.lab2.dto.EventLocationJson
import com.ndbx.lab2.dto.EventReactionsJson
import com.ndbx.lab2.dto.UserJson

fun EventDocument.toJson(reactions: EventReactionsJson? = null) = EventListItemJson(
    id = id!!,
    title = title,
    category = category,
    price = price,
    description = description,
    location = EventLocationJson(
        address = location.address,
        city = location.city,
    ),
    createdAt = createdAt,
    createdBy = createdBy,
    startedAt = startedAt,
    finishedAt = finishedAt,
    reactions = reactions,
)

fun UserDocument.toJson() = UserJson(
    id = id!!,
    fullName = fullName,
    username = username,
)
