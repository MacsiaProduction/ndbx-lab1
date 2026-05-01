package com.ndbx.lab2.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cassandra")
data class AppCassandraProperties(
    val hosts: String = "localhost",
    val port: Int = 9042,
    val username: String = "",
    val password: String = "",
    val keyspace: String = "testkeyspace",
    val consistency: String = "ONE",
    val localDatacenter: String = "datacenter1",
)
