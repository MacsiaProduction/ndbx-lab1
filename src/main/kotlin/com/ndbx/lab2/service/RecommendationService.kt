package com.ndbx.lab2.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ndbx.lab2.config.AppRecommendationsProperties
import com.ndbx.lab2.document.EventDocument
import com.ndbx.lab2.dto.EventListItemJson
import com.ndbx.lab2.repository.EventRepository
import com.ndbx.lab2.repository.RecommendationGraphRepository
import com.ndbx.lab2.support.toJson
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RecommendationService(
    private val recommendationGraphRepository: RecommendationGraphRepository,
    private val eventRepository: EventRepository,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val recommendationsProperties: AppRecommendationsProperties,
) {
    private val eventListType =
        objectMapper.typeFactory.constructCollectionType(List::class.java, EventListItemJson::class.java)

    fun recommendFor(userId: String): List<EventListItemJson> {
        val key = redisKey(userId)
        val hashOps = redisTemplate.opsForHash<String, String>()
        hashOps.get(key, EVENTS_FIELD)?.let { return objectMapper.readValue(it, eventListType) }
        val events = build(userId)
        hashOps.put(key, EVENTS_FIELD, objectMapper.writeValueAsString(events))
        redisTemplate.expire(key, Duration.ofSeconds(recommendationsProperties.ttl))
        return events
    }

    private fun build(userId: String): List<EventListItemJson> {
        val ranked = recommendationGraphRepository.findRecommendations(userId)
        if (ranked.isEmpty()) return emptyList()
        val popularityById = ranked.associate { it.id to it.popularity }
        return eventRepository.findAllById(popularityById.keys)
            .groupBy { it.title }
            .map { (_, group) -> group.minBy(EventDocument::startedAt) }
            .sortedWith(compareByDescending<EventDocument> { popularityById[it.id] ?: 0L }.thenBy { it.title })
            .map { it.toJson() }
    }

    private fun redisKey(userId: String) = "user:$userId:recomms"

    private companion object {
        private const val EVENTS_FIELD = "events"
    }
}
