package com.ndbx.lab2.support

fun wantsReactions(include: String?): Boolean =
    include
        ?.split(',')
        ?.any { it.trim().equals("reactions", ignoreCase = true) }
        ?: false
