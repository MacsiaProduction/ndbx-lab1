package com.ndbx.lab2.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@ConfigurationProperties(prefix = "app.mongodb")
data class AppMongoProperties(
    var database: String = "eventhub",
    var user: String = "",
    var password: String = "",
    var host: String = "localhost",
    var port: Int = 27017,
) {
    fun connectionUri(): String {
        val u = user.trim()
        val p = password.trim()
        return if (u.isNotEmpty() && p.isNotEmpty()) {
            val eu = URLEncoder.encode(u, StandardCharsets.UTF_8)
            val ep = URLEncoder.encode(p, StandardCharsets.UTF_8)
            "mongodb://$eu:$ep@$host:$port/$database?authSource=admin"
        } else {
            "mongodb://$host:$port/$database"
        }
    }
}
