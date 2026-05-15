package com.ndbx.lab2.support

import com.ndbx.lab2.service.EventSearchCriteria
import com.ndbx.lab2.service.UserSearchCriteria
import java.time.LocalDate

object SearchRequestSupport {
    data class EventParseResult(
        val criteria: EventSearchCriteria? = null,
        val invalidField: String? = null,
    )

    data class UserParseResult(
        val criteria: UserSearchCriteria? = null,
        val invalidField: String? = null,
    )

    fun parseEventSearchCriteria(
        idRaw: String? = null,
        titleRaw: String? = null,
        categoryRaw: String? = null,
        priceFromRaw: String? = null,
        priceToRaw: String? = null,
        addressRaw: String? = null,
        cityRaw: String? = null,
        dateFromRaw: String? = null,
        dateToRaw: String? = null,
        userIdRaw: String? = null,
        userRaw: String? = null,
        limitRaw: String? = null,
        offsetRaw: String? = null,
        createdByUserIdOverride: String? = null,
    ): EventParseResult {
        val id = parseOptionalText(idRaw, "id").orInvalid { return invalidEvent(it) }
        val title = parseOptionalText(titleRaw, "title").orInvalid { return invalidEvent(it) }
        val address = parseOptionalText(addressRaw, "address").orInvalid { return invalidEvent(it) }
        val city = parseOptionalText(cityRaw, "city").orInvalid { return invalidEvent(it) }
        val userId = parseOptionalText(userIdRaw, "user_id").orInvalid { return invalidEvent(it) }
        val user = parseOptionalText(userRaw, "user").orInvalid { return invalidEvent(it) }
        val category = parseCategory(categoryRaw).orInvalid { return invalidEvent(it) }
        val limit = parseOptionalUInt(limitRaw, "limit").orInvalid { return invalidEvent(it) }
        val offset = parseOptionalUInt(offsetRaw, "offset").orInvalid { return invalidEvent(it) }
        val priceFrom = parseOptionalUInt(priceFromRaw, "price_from").orInvalid { return invalidEvent(it) }
        val priceTo = parseOptionalUInt(priceToRaw, "price_to").orInvalid { return invalidEvent(it) }
        val dateFrom = parseOptionalDate(dateFromRaw, "date_from").orInvalid { return invalidEvent(it) }
        val dateTo = parseOptionalDate(dateToRaw, "date_to").orInvalid { return invalidEvent(it) }
        if (priceFrom != null && priceTo != null && priceFrom > priceTo) {
            return invalidEvent("price_to")
        }
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            return invalidEvent("date_to")
        }
        return EventParseResult(
            criteria = EventSearchCriteria(
                id = id,
                title = title,
                category = category,
                priceFrom = priceFrom,
                priceTo = priceTo,
                address = address,
                city = city,
                dateFrom = dateFrom,
                dateTo = dateTo,
                createdByUserId = createdByUserIdOverride ?: userId,
                createdByUsername = user,
                limit = limit,
                offset = offset ?: 0,
            ),
        )
    }

    fun parseUserSearchCriteria(
        idRaw: String? = null,
        nameRaw: String? = null,
        limitRaw: String? = null,
        offsetRaw: String? = null,
    ): UserParseResult {
        val id = parseOptionalText(idRaw, "id").orInvalid { return invalidUser(it) }
        val name = parseOptionalText(nameRaw, "name").orInvalid { return invalidUser(it) }
        val limit = parseOptionalUInt(limitRaw, "limit").orInvalid { return invalidUser(it) }
        val offset = parseOptionalUInt(offsetRaw, "offset").orInvalid { return invalidUser(it) }
        return UserParseResult(
            criteria = UserSearchCriteria(
                id = id,
                name = name,
                limit = limit,
                offset = offset ?: 0,
            ),
        )
    }

    private fun parseOptionalText(raw: String?, fieldName: String): ParsedValue<String> {
        if (raw == null) return ParsedValue()
        val value = raw.trim()
        if (value.isEmpty()) return ParsedValue(invalidField = fieldName)
        return ParsedValue(value = value)
    }

    private fun parseCategory(raw: String?): ParsedValue<String> {
        if (raw == null) return ParsedValue()
        val value = raw.trim()
        if (value.isEmpty()) return ParsedValue(invalidField = "category")
        val category = EventCategory.from(value)
        return ParsedValue(
            value = category?.value,
            invalidField = if (category == null) "category" else null,
        )
    }

    private fun parseOptionalUInt(raw: String?, fieldName: String): ParsedValue<Int> {
        val parsed = RequestSupport.optionalNonNegativeUInt(fieldName, raw)
        return ParsedValue(
            value = parsed.value,
            invalidField = parsed.invalidParameter,
        )
    }

    private fun parseOptionalDate(raw: String?, fieldName: String): ParsedValue<LocalDate> {
        if (raw == null) return ParsedValue()
        val value = raw.trim()
        if (value.isEmpty()) return ParsedValue(invalidField = fieldName)
        val parsedDate = RequestSupport.parseCompactDate(value)
        return ParsedValue(
            value = parsedDate,
            invalidField = if (parsedDate == null) fieldName else null,
        )
    }

    private fun invalidEvent(field: String) = EventParseResult(invalidField = field)

    private fun invalidUser(field: String) = UserParseResult(invalidField = field)

    private data class ParsedValue<T>(
        val value: T? = null,
        val invalidField: String? = null,
    ) {
        inline fun orInvalid(onInvalid: (String) -> Nothing): T? {
            invalidField?.let(onInvalid)
            return value
        }
    }
}
