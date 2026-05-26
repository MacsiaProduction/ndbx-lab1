package com.ndbx.lab2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.recommendations")
data class AppRecommendationsProperties(
    val ttl: Long = 60,
)
