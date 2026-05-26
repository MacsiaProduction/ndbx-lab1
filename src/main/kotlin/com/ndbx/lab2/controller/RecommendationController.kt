package com.ndbx.lab2.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.ndbx.lab2.dto.EventListItemJson
import com.ndbx.lab2.service.RecommendationService
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

data class RecommendationsResponse(
    @JsonProperty("events") val events: List<EventListItemJson>,
)

@RestController
class RecommendationController(
    private val recommendationService: RecommendationService,
    private val sessionService: SessionService,
) {
    @GetMapping("/recommendations")
    fun get(
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val sid = sessionService.resolveSession(sidCookie)
        val userId = sid?.let(sessionService::getUserId)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        SessionCookies.refreshSession(response, sidCookie, sessionService)
        return ResponseEntity.ok(RecommendationsResponse(recommendationService.recommendFor(userId)))
    }
}
