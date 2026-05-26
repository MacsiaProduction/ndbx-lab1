package com.ndbx.lab2.repository

import org.neo4j.driver.Driver
import org.springframework.stereotype.Repository

data class RecommendedEvent(val id: String, val popularity: Long)

@Repository
class RecommendationGraphRepository(
    private val neo4jDriver: Driver,
) {
    fun recordLike(userId: String, eventId: String, eventTitle: String) {
        neo4jDriver.session().use { session ->
            session.run(
                """
                MERGE (u:User {id: ${'$'}uid})
                MERGE (e:Event {id: ${'$'}eid}) SET e.title = ${'$'}title
                MERGE (u)-[:LIKED]->(e)
                """.trimIndent(),
                mapOf("uid" to userId, "eid" to eventId, "title" to eventTitle),
            ).consume()
        }
    }

    fun findRecommendations(userId: String): List<RecommendedEvent> =
        neo4jDriver.session().use { session ->
            session.run(
                """
                MATCH (me:User {id: ${'$'}uid})-[:LIKED]->(:Event)<-[:LIKED]-(other:User)-[:LIKED]->(rec:Event)
                WHERE other <> me AND NOT (me)-[:LIKED]->(rec)
                RETURN rec.id AS id, count(*) AS popularity
                ORDER BY popularity DESC, rec.id ASC
                """.trimIndent(),
                mapOf("uid" to userId),
            ).list { rec -> RecommendedEvent(rec["id"].asString(), rec["popularity"].asLong()) }
        }
}
