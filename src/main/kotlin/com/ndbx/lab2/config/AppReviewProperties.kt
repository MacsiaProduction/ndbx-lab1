package com.ndbx.lab2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.reviews")
data class AppReviewProperties(
    val ttl: Long = 120,
)
