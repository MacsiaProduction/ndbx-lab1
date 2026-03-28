package com.ndbx.lab2.service

import com.ndbx.lab2.config.AppSessionProperties
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.output.IntegerOutput
import org.springframework.data.redis.connection.DecoratedRedisConnection
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.lettuce.LettuceConnection
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@Service
class SessionService(
    private val redisTemplate: StringRedisTemplate,
    private val sessionProperties: AppSessionProperties
) {
    companion object {
        private const val SESSION_PREFIX = "sid:"
        private val SID_PATTERN = Regex("^[0-9a-f]{32}$")
    }

    fun generateSid() = UUID.randomUUID().toString().replace("-", "")

    fun isValidSid(sid: String) = SID_PATTERN.matches(sid)

    fun createSession(sid: String): Boolean {
        val key = key(sid)
        val now = Instant.now().toString()
        val ttl = sessionProperties.ttl.toString()
        val reply = redisTemplate.execute(RedisCallback { conn: RedisConnection ->
            hsetex(
                conn,
                utf8(key), utf8("FNX"),
                utf8("EX"), utf8(ttl),
                utf8("FIELDS"), utf8("2"),
                utf8("created_at"), utf8(now),
                utf8("updated_at"), utf8(now),
            )
        })
        return hsetexOk(reply)
    }

    fun createUniqueSession(): String {
        repeat(5) {
            val sid = generateSid()
            if (createSession(sid)) return sid
        }
        error("Failed to generate a unique session id after retries")
    }

    fun touchSession(sid: String): Boolean {
        val key = key(sid)
        val now = Instant.now().toString()
        val ttl = sessionProperties.ttl.toString()
        val reply = redisTemplate.execute(RedisCallback { conn: RedisConnection ->
            hsetex(
                conn,
                utf8(key), utf8("FXX"),
                utf8("EX"), utf8(ttl),
                utf8("FIELDS"), utf8("1"),
                utf8("updated_at"), utf8(now),
            )
        })
        return hsetexOk(reply)
    }

    fun getTtl() = sessionProperties.ttl

    private fun key(sid: String) = "$SESSION_PREFIX$sid"

    private fun utf8(s: String) = s.toByteArray(StandardCharsets.UTF_8)

    private fun hsetex(conn: RedisConnection, vararg args: ByteArray): Any? {
        val raw = when (conn) {
            is DecoratedRedisConnection -> conn.delegate
            else -> conn
        }
        check(raw is LettuceConnection) { "Ожидается Lettuce; для другого драйвера нужен свой вызов HSETEX" }
        return raw.execute("HSETEX", IntegerOutput(ByteArrayCodec.INSTANCE), *args)
    }

    private fun hsetexOk(reply: Any?) = (reply as? Number)?.toLong() == 1L
}
