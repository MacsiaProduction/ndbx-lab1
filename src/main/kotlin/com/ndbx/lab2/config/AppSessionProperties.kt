package com.ndbx.lab2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.session")
data class AppSessionProperties(
    val ttl: Long = 3600
)
