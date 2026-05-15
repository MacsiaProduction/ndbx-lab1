package com.ndbx.lab2.service

import com.datastax.oss.driver.api.core.CqlSession
import com.ndbx.lab2.config.AppReviewProperties
import com.ndbx.lab2.dto.EventReviewsJson
import com.ndbx.lab2.dto.ReviewListItemJson
import com.ndbx.lab2.repository.EventRepository
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Service
class EventReviewService(
    private val cqlSession: CqlSession,
    private val eventRepository: EventRepository,
    private val redisTemplate: StringRedisTemplate,
    private val reviewProperties: AppReviewProperties,
) {

    private val insertIfNotExistsPrepared by lazy {
        cqlSession.prepare(
            """
            INSERT INTO event_reviews (event_id, created_by, id, rating, comment, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            IF NOT EXISTS
            """.trimIndent(),
        )
    }

    private val selectByEventPrepared by lazy {
        cqlSession.prepare(
            """
            SELECT id, event_id, rating, comment, created_at, updated_at, created_by
            FROM event_reviews WHERE event_id = ?
            """.trimIndent(),
        )
    }

    private val selectByIdPrepared by lazy {
        cqlSession.prepare(
            """
            SELECT event_id, created_by, id, rating, comment, created_at, updated_at
            FROM event_reviews WHERE event_id = ? AND id = ? ALLOW FILTERING
            """.trimIndent(),
        )
    }

    private val updatePrepared by lazy {
        cqlSession.prepare(
            """
            UPDATE event_reviews SET comment = ?, rating = ?, updated_at = ?
            WHERE event_id = ? AND created_by = ?
            """.trimIndent(),
        )
    }

    fun tryCreateReview(
        eventId: String,
        userId: String,
        comment: String,
        rating: Int,
        eventTitle: String,
    ): UUID? {
        val id = UUID.randomUUID()
        val now = Instant.now()
        val rs = cqlSession.execute(
            insertIfNotExistsPrepared.bind(
                eventId,
                userId,
                id,
                rating.toByte(),
                comment,
                now,
                now,
            ),
        )
        if (!rs.wasApplied()) {
            return null
        }
        refreshRedisCacheForTitle(eventTitle)
        return id
    }

    fun tryUpdateReview(
        eventId: String,
        reviewId: UUID,
        userId: String,
        comment: String?,
        rating: Int?,
        eventTitle: String,
    ): Boolean {
        val row = cqlSession.execute(selectByIdPrepared.bind(eventId, reviewId)).one() ?: return false
        val rowCreatedBy = row.getString("created_by") ?: return false
        if (rowCreatedBy != userId) {
            return false
        }
        val newComment = comment ?: (row.getString("comment") ?: "")
        val newRating = rating ?: row.getByte("rating").toInt()
        val now = Instant.now()
        cqlSession.execute(
            updatePrepared.bind(
                newComment,
                newRating.toByte(),
                now,
                eventId,
                userId,
            ),
        )
        refreshRedisCacheForTitle(eventTitle)
        return true
    }

    fun listReviewsForEvent(eventId: String, limit: Int, offset: Int): List<ReviewListItemJson> {
        val rs = cqlSession.execute(selectByEventPrepared.bind(eventId))
        val rows = buildList {
            for (row in rs) {
                val createdAt = row.getInstant("created_at")!!
                val updatedAt = row.getInstant("updated_at")!!
                add(
                    ReviewListItemJson(
                        id = row.getUuid("id").toString(),
                        eventId = row.getString("event_id")!!,
                        comment = row.getString("comment")!!,
                        createdAt = formatInstant(createdAt),
                        createdBy = row.getString("created_by")!!,
                        rating = row.getByte("rating").toInt(),
                        updatedAt = formatInstant(updatedAt),
                    ),
                )
            }
        }
        return rows.drop(offset).take(limit)
    }

    fun getReviewsAggregateForTitle(title: String): EventReviewsJson {
        val key = redisKey(title)
        val countStr = redisTemplate.opsForHash<String, String>().get(key, REDIS_FIELD_COUNT)
        val ratingStr = redisTemplate.opsForHash<String, String>().get(key, REDIS_FIELD_RATING)
        if (countStr != null && ratingStr != null) {
            return EventReviewsJson(countStr.toInt(), ratingStr.toDouble())
        }
        val aggregate = aggregateReviewsFromCassandra(title)
        if (aggregate.count > 0) {
            writeReviewsHash(key, aggregate)
        }
        return aggregate
    }

    private fun refreshRedisCacheForTitle(title: String) {
        val key = redisKey(title)
        val aggregate = aggregateReviewsFromCassandra(title)
        if (aggregate.count > 0) {
            writeReviewsHash(key, aggregate)
        } else {
            redisTemplate.delete(key)
        }
    }

    private fun writeReviewsHash(key: String, aggregate: EventReviewsJson) {
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                REDIS_FIELD_COUNT to aggregate.count.toString(),
                REDIS_FIELD_RATING to String.format(Locale.US, "%.1f", aggregate.rating),
            ),
        )
        redisTemplate.expire(key, Duration.ofSeconds(reviewProperties.ttl))
    }

    private fun aggregateReviewsFromCassandra(title: String): EventReviewsJson {
        val events = eventRepository.findByTitle(title)
        var count = 0
        var sum = 0L
        for (e in events) {
            val id = e.id ?: continue
            val rs = cqlSession.execute(selectByEventPrepared.bind(id))
            for (_row in rs) {
                count++
                sum += _row.getByte("rating").toInt()
            }
        }
        if (count == 0) {
            return EventReviewsJson(0, 0.0)
        }
        val avg = sum.toDouble() / count
        val rounded = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).toDouble()
        return EventReviewsJson(count, rounded)
    }

    private fun redisKey(title: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hex = md.digest(title.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
        return "event:$hex:reviews"
    }

    private fun formatInstant(instant: Instant): String =
        java.time.OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private companion object {
        private const val REDIS_FIELD_COUNT = "count"
        private const val REDIS_FIELD_RATING = "rating"
    }
}
