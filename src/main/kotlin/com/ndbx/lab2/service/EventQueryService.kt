package com.ndbx.lab2.service

import com.ndbx.lab2.document.EventDocument
import com.ndbx.lab2.document.UserDocument
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

data class EventSearchCriteria(
    val id: String? = null,
    val title: String? = null,
    val category: String? = null,
    val priceFrom: Int? = null,
    val priceTo: Int? = null,
    val address: String? = null,
    val city: String? = null,
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val createdByUserId: String? = null,
    val createdByUsername: String? = null,
    val limit: Int? = null,
    val offset: Int = 0,
)

@Service
class EventQueryService(
    private val mongoTemplate: MongoTemplate,
) {
    fun findFiltered(criteria: EventSearchCriteria): List<EventDocument> {
        val resolvedUserId = criteria.createdByUsername?.let { username ->
            mongoTemplate.findOne(
                Query.query(Criteria.where("username").`is`(username)),
                UserDocument::class.java,
            )?.id ?: return emptyList()
        }
        val createdByUserId = when {
            criteria.createdByUserId == null -> resolvedUserId
            resolvedUserId == null -> criteria.createdByUserId
            criteria.createdByUserId == resolvedUserId -> criteria.createdByUserId
            else -> return emptyList()
        }
        val filters = buildList {
            criteria.id?.let { add(Criteria.where("_id").`is`(it)) }
            criteria.title?.let { add(Criteria.where("title").regex(containsPattern(it))) }
            criteria.category?.let { add(Criteria.where("category").`is`(it)) }
            criteria.address?.let { add(Criteria.where("location.address").`is`(it)) }
            criteria.city?.let { add(Criteria.where("location.city").`is`(it)) }
            createdByUserId?.let { add(Criteria.where("created_by").`is`(it)) }
            buildPriceCriteria(criteria.priceFrom, criteria.priceTo)?.let(::add)
            buildDateCriteria(criteria.dateFrom, criteria.dateTo)?.let(::add)
        }
        val query = Query()
        if (filters.isNotEmpty()) {
            query.addCriteria(
                if (filters.size == 1) {
                    filters.first()
                } else {
                    Criteria().andOperator(*filters.toTypedArray())
                },
            )
        }
        query.with(
            Sort.by(
                Sort.Order.asc("created_at"),
                Sort.Order.asc("started_at"),
                Sort.Order.asc("_id"),
            ),
        ).skip(criteria.offset.toLong())
        criteria.limit?.let(query::limit)
        return mongoTemplate.find(query, EventDocument::class.java)
    }

    private fun buildPriceCriteria(priceFrom: Int?, priceTo: Int?): Criteria? {
        if (priceFrom == null && priceTo == null) return null
        return Criteria.where("price").apply {
            priceFrom?.let(::gte)
            priceTo?.let(::lte)
        }
    }

    private fun buildDateCriteria(dateFrom: LocalDate?, dateTo: LocalDate?): Criteria? {
        if (dateFrom == null && dateTo == null) return null
        return Criteria.where("started_at").apply {
            dateFrom?.let { gte(it.format(DateTimeFormatter.ISO_LOCAL_DATE)) }
            dateTo?.let { lte("${it.format(DateTimeFormatter.ISO_LOCAL_DATE)}~") }
        }
    }

    private fun containsPattern(value: String): Pattern =
        Pattern.compile(
            Pattern.quote(value),
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE,
        )
}
