package com.ndbx.lab2.service

import com.ndbx.lab2.document.UserDocument
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.util.regex.Pattern

data class UserSearchCriteria(
    val id: String? = null,
    val name: String? = null,
    val limit: Int? = null,
    val offset: Int = 0,
)

@Service
class UserQueryService(
    private val mongoTemplate: MongoTemplate,
) {
    fun findFiltered(criteria: UserSearchCriteria): List<UserDocument> {
        val filters = buildList {
            criteria.id?.let { add(Criteria.where("_id").`is`(it)) }
            criteria.name?.let { add(Criteria.where("full_name").regex(containsPattern(it))) }
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
                Sort.Order.asc("full_name"),
                Sort.Order.asc("username"),
                Sort.Order.asc("_id"),
            ),
        ).skip(criteria.offset.toLong())
        criteria.limit?.let(query::limit)
        return mongoTemplate.find(query, UserDocument::class.java)
    }

    private fun containsPattern(value: String): Pattern =
        Pattern.compile(
            Pattern.quote(value),
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE,
        )
}
