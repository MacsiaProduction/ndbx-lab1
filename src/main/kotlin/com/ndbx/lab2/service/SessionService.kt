package com.ndbx.lab2.service

import com.ndbx.lab2.config.AppSessionProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class SessionService(
    private val redisTemplate: StringRedisTemplate,
    private val sessionProperties: AppSessionProperties,
) {
    companion object {
        private const val SESSION_PREFIX = "sid:"
        private val SID_PATTERN = Regex("^[0-9a-f]{32}$")
        const val CREATED_AT_FIELD = "created_at"
        const val UPDATED_AT_FIELD = "updated_at"
        const val USER_ID_FIELD = "user_id"
    }

    fun generateSid() = UUID.randomUUID().toString().replace("-", "")

    fun isValidSid(sid: String) = SID_PATTERN.matches(sid)

    fun resolveSession(sidCookie: String?): String? =
        sidCookie?.takeIf { isValidSid(it) && sessionExists(it) }

    fun createSession(sid: String): Boolean {
        val key = key(sid)
        if (sessionExists(sid)) return false
        val now = Instant.now().toString()
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                CREATED_AT_FIELD to now,
                UPDATED_AT_FIELD to now,
            ),
        )
        redisTemplate.expire(key, ttlDuration())
        return true
    }

    fun createUniqueSession(): String {
        repeat(5) {
            val sid = generateSid()
            if (createSession(sid)) return sid
        }
        error("Failed to generate a unique session id after retries")
    }

    fun createSessionWithUser(userId: String): String {
        repeat(5) {
            val sid = generateSid()
            val k = key(sid)
            if (sessionExists(sid)) return@repeat
            val now = Instant.now().toString()
            redisTemplate.opsForHash<String, String>().putAll(
                k,
                mapOf(
                    CREATED_AT_FIELD to now,
                    UPDATED_AT_FIELD to now,
                    USER_ID_FIELD to userId,
                ),
            )
            redisTemplate.expire(k, ttlDuration())
            return sid
        }
        error("Failed to generate a unique session id after retries")
    }

    fun bindUserToSession(sid: String, userId: String): Boolean {
        if (!sessionExists(sid)) return false
        val k = key(sid)
        val now = Instant.now().toString()
        redisTemplate.opsForHash<String, String>().putAll(
            k,
            mapOf(
                USER_ID_FIELD to userId,
                UPDATED_AT_FIELD to now,
            ),
        )
        redisTemplate.expire(k, ttlDuration())
        return true
    }

    fun touchSession(sid: String): Boolean {
        if (!sessionExists(sid)) return false
        val k = key(sid)
        val now = Instant.now().toString()
        redisTemplate.opsForHash<String, String>().put(key(sid), UPDATED_AT_FIELD, now)
        redisTemplate.expire(k, ttlDuration())
        return true
    }

    fun sessionExists(sid: String): Boolean =
        redisTemplate.hasKey(key(sid))

    fun getUserId(sid: String): String? {
        val v = redisTemplate.opsForHash<String, String>().get(key(sid), USER_ID_FIELD)
        return v?.takeIf { it.isNotBlank() }
    }

    fun deleteSession(sid: String): Boolean =
        redisTemplate.delete(key(sid))

    fun getTtl() = sessionProperties.ttl

    private fun key(sid: String) = "$SESSION_PREFIX$sid"

    private fun ttlDuration() = Duration.ofSeconds(sessionProperties.ttl)
}
