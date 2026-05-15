package com.ndbx.lab2.support

enum class EventCategory(val value: String) {
    MEETUP("meetup"),
    CONCERT("concert"),
    EXHIBITION("exhibition"),
    PARTY("party"),
    OTHER("other");

    companion object {
        fun from(value: String?): EventCategory? =
            entries.firstOrNull { it.value == value }
    }
}
