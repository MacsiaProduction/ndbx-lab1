package com.ndbx.lab2.support

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object RequestSupport {

    data class OptionalUInt(val value: Int?, val invalidParameter: String?)

    fun optionalNonNegativeUInt(parameterName: String, raw: String?): OptionalUInt {
        if (raw == null) return OptionalUInt(null, null)
        val v = raw.toIntOrNull() ?: return OptionalUInt(null, parameterName)
        if (v < 0) return OptionalUInt(null, parameterName)
        return OptionalUInt(v, null)
    }

    fun parseRfc3339(s: String): OffsetDateTime? =
        try {
            OffsetDateTime.parse(s)
        } catch (_: DateTimeParseException) {
            null
        }

    fun parseCompactDate(s: String): LocalDate? =
        try {
            LocalDate.parse(s, DateTimeFormatter.BASIC_ISO_DATE)
        } catch (_: DateTimeParseException) {
            null
        }
}
