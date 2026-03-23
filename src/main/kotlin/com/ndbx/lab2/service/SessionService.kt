package com.ndbx.lab2.service

import com.ndbx.lab2.config.AppSessionProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

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
        val hashOps = redisTemplate.opsForHash<String, String>()
        val created = hashOps.putIfAbsent(key, "created_at", now)
        if (created) {
            hashOps.put(key, "updated_at", now)
            redisTemplate.expire(key, sessionProperties.ttl, TimeUnit.SECONDS)
        }
        return created
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
        if (!redisTemplate.hasKey(key)) return false
        redisTemplate.opsForHash<String, String>().put(key, "updated_at", Instant.now().toString())
        redisTemplate.expire(key, sessionProperties.ttl, TimeUnit.SECONDS)
        return true
    }

    fun getTtl() = sessionProperties.ttl

    private fun key(sid: String) = "$SESSION_PREFIX$sid"
}
