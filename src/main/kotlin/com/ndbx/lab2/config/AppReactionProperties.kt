package com.ndbx.lab2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.reactions")
data class AppReactionProperties(
    val ttl: Long = 60,
)
