package com.ndbx.lab2.service

import com.ndbx.lab2.config.AppSessionProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SessionService(
    private val redisTemplate: StringRedisTemplate,
    private val sessionProperties: AppSessionProperties
) {
    private val ttlSeconds = sessionProperties.ttl.toString()

    companion object {
        private const val SESSION_PREFIX = "sid:"
        private val SID_PATTERN = Regex("^[0-9a-f]{32}$")

        private val CREATE_SESSION_SCRIPT: RedisScript<Long> = RedisScript.of(
            """
            if redis.call('EXISTS', KEYS[1]) == 0 then
              redis.call('HSET', KEYS[1], 'created_at', ARGV[1], 'updated_at', ARGV[1])
              redis.call('EXPIRE', KEYS[1], ARGV[2])
              return 1
            end
            return 0
            """.trimIndent(),
            Long::class.java
        )

        private val TOUCH_SESSION_SCRIPT: RedisScript<Long> = RedisScript.of(
            """
            if redis.call('EXISTS', KEYS[1]) == 1 then
              redis.call('HSET', KEYS[1], 'updated_at', ARGV[1])
              redis.call('EXPIRE', KEYS[1], ARGV[2])
              return 1
            end
            return 0
            """.trimIndent(),
            Long::class.java
        )
    }

    fun generateSid() = UUID.randomUUID().toString().replace("-", "")

    fun isValidSid(sid: String) = SID_PATTERN.matches(sid)

    fun createSession(sid: String) = runScript(CREATE_SESSION_SCRIPT, sid)

    fun createUniqueSession(): String {
        repeat(5) {
            val sid = generateSid()
            if (createSession(sid)) return sid
        }
        error("Failed to generate a unique session id after retries")
    }

    fun touchSession(sid: String) = runScript(TOUCH_SESSION_SCRIPT, sid)

    fun getTtl() = sessionProperties.ttl

    private fun key(sid: String) = "$SESSION_PREFIX$sid"

    private fun runScript(script: RedisScript<Long>, sid: String) =
        redisTemplate.execute(script, listOf(key(sid)), Instant.now().toString(), ttlSeconds) == 1L
}
