package com.ndbx.lab2.config

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.DefaultDriverOption
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetSocketAddress
import java.time.Duration

@Configuration
class CassandraConfiguration(
    private val cassandraProperties: AppCassandraProperties,
) {

    @Bean(destroyMethod = "close")
    fun cqlSession(): CqlSession {
        val props = cassandraProperties
        val hosts = props.hosts.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        require(hosts.isNotEmpty()) { "CASSANDRA_HOSTS must list at least one host" }

        val loader = DriverConfigLoader.programmaticBuilder()
            .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(10))
            .withString(DefaultDriverOption.REQUEST_CONSISTENCY, props.consistency.uppercase())
            .build()

        val builder = CqlSession.builder()
            .withConfigLoader(loader)
            .withLocalDatacenter(props.localDatacenter)
            .withKeyspace(props.keyspace)

        if (props.username.isNotBlank()) {
            builder.withAuthCredentials(props.username, props.password)
        }

        hosts.forEach { host ->
            builder.addContactPoint(InetSocketAddress(host, props.port))
        }

        return builder.build()
    }
}
