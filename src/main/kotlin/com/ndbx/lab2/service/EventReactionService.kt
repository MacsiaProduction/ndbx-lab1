package com.ndbx.lab2.service

import com.datastax.oss.driver.api.core.CqlSession
import com.ndbx.lab2.config.AppReactionProperties
import com.ndbx.lab2.dto.EventReactionsJson
import com.ndbx.lab2.repository.EventRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant

@Service
class EventReactionService(
    private val cqlSession: CqlSession,
    private val eventRepository: EventRepository,
    private val redisTemplate: StringRedisTemplate,
    private val reactionProperties: AppReactionProperties,
) {

    private val insertPrepared by lazy {
        cqlSession.prepare(
            """
            INSERT INTO event_reactions (event_id, created_by, like_value, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
        )
    }

    private val selectByEventPrepared by lazy {
        cqlSession.prepare(
            """
            SELECT like_value FROM event_reactions WHERE event_id = ?
            """.trimIndent(),
        )
    }

    fun likeEvent(eventId: String, userId: String, eventTitle: String) {
        upsertReaction(eventId, userId, LIKE_VALUE, eventTitle)
    }

    fun dislikeEvent(eventId: String, userId: String, eventTitle: String) {
        upsertReaction(eventId, userId, DISLIKE_VALUE, eventTitle)
    }

    fun getReactionsForTitle(title: String): EventReactionsJson {
        val key = redisKey(title)
        val likesStr = redisTemplate.opsForHash<String, String>().get(key, REDIS_FIELD_LIKES)
        val dislikesStr = redisTemplate.opsForHash<String, String>().get(key, REDIS_FIELD_DISLIKES)
        if (likesStr != null && dislikesStr != null) {
            return EventReactionsJson(likesStr.toInt(), dislikesStr.toInt())
        }
        val aggregate = aggregateFromCassandra(title)
        if (aggregate.likes != 0 || aggregate.dislikes != 0) {
            writeReactionHash(key, aggregate)
        }
        return aggregate
    }

    private fun upsertReaction(eventId: String, userId: String, likeValue: Byte, eventTitle: String) {
        val bound = insertPrepared.bind(eventId, userId, likeValue, Instant.now())
        cqlSession.execute(bound)
        refreshRedisCacheForTitle(eventTitle)
    }

    private fun refreshRedisCacheForTitle(title: String) {
        val aggregate = aggregateFromCassandra(title)
        writeReactionHash(redisKey(title), aggregate)
    }

    private fun writeReactionHash(key: String, aggregate: EventReactionsJson) {
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                REDIS_FIELD_LIKES to aggregate.likes.toString(),
                REDIS_FIELD_DISLIKES to aggregate.dislikes.toString(),
            ),
        )
        redisTemplate.expire(key, Duration.ofSeconds(reactionProperties.ttl))
    }

    private fun aggregateFromCassandra(title: String): EventReactionsJson {
        val events = eventRepository.findByTitle(title)
        var likes = 0
        var dislikes = 0
        for (e in events) {
            val id = e.id ?: continue
            val rs = cqlSession.execute(selectByEventPrepared.bind(id))
            for (row in rs) {
                val v = row.getByte("like_value")
                when (v) {
                    LIKE_VALUE -> likes++
                    DISLIKE_VALUE -> dislikes++
                }
            }
        }
        return EventReactionsJson(likes, dislikes)
    }

    private fun redisKey(title: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hex = md.digest(title.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
        return "event:$hex:reactions"
    }

    private companion object {
        private const val REDIS_FIELD_LIKES = "likes"
        private const val REDIS_FIELD_DISLIKES = "dislikes"

        private val LIKE_VALUE: Byte = 1.toByte()
        private val DISLIKE_VALUE: Byte = (-1).toByte()
    }
}
