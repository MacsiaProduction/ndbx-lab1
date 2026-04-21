package com.ndbx.lab2.service

import com.ndbx.lab2.document.EventDocument
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service
class EventQueryService(
    private val mongoTemplate: MongoTemplate,
) {
    fun findFiltered(titleSubstring: String?, limit: Int?, offset: Int): List<EventDocument> {
        var query = if (!titleSubstring.isNullOrBlank()) {
            val safe = Pattern.quote(titleSubstring)
            Query.query(Criteria.where("title").regex(Pattern.compile(safe, Pattern.CASE_INSENSITIVE)))
        } else {
            Query()
        }
        query = query.with(Sort.by(Sort.Direction.ASC, "created_at")).skip(offset.toLong())
        if (limit != null) {
            query = query.limit(limit)
        }
        return mongoTemplate.find(query, EventDocument::class.java)
    }
}
