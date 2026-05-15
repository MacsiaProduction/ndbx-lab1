package com.ndbx.lab2.controller

import com.ndbx.lab2.dto.CreateReviewRequest
import com.ndbx.lab2.dto.CreateReviewResponse
import com.ndbx.lab2.dto.MessageResponse
import com.ndbx.lab2.dto.ReviewListResponse
import com.ndbx.lab2.dto.UpdateReviewRequest
import com.ndbx.lab2.repository.EventRepository
import com.ndbx.lab2.service.EventReviewService
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.support.RequestSupport
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class EventReviewController(
    private val eventRepository: EventRepository,
    private val eventReviewService: EventReviewService,
    private val sessionService: SessionService,
) {
    @PostMapping("/events/{event_id}/reviews")
    fun createReview(
        @PathVariable("event_id") eventId: String,
        @RequestBody req: CreateReviewRequest,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val sid = sessionService.resolveSession(sidCookie)
        val userId = sid?.let(sessionService::getUserId)
        if (sid == null || userId == null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        }
        val event = eventRepository.findById(eventId).orElse(null)
        if (event == null) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Event not found"))
        }
        val badField = validateCreateReview(req)
        if (badField != null) {
            touchSession(response, sid)
            return ResponseEntity.badRequest()
                .body(MessageResponse("""invalid "$badField" field"""))
        }
        val comment = req.comment!!.trim()
        val rating = req.rating!!
        val newId = eventReviewService.tryCreateReview(
            eventId = eventId,
            userId = userId,
            comment = comment,
            rating = rating,
            eventTitle = event.title,
        )
        if (newId == null) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MessageResponse("Already exists"))
        }
        touchSession(response, sid)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CreateReviewResponse(id = newId.toString()))
    }

    @GetMapping("/events/{event_id}/reviews")
    fun listReviews(
        @PathVariable("event_id") eventId: String,
        @RequestParam(name = "limit", required = false) limitRaw: String?,
        @RequestParam(name = "offset", required = false) offsetRaw: String?,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val limitParsed = RequestSupport.optionalNonNegativeUInt("limit", limitRaw)
        val limitInvalid = limitParsed.invalidParameter
        if (limitInvalid != null) {
            return listQueryParamError(response, sidCookie, limitInvalid)
        }
        val offsetParsed = RequestSupport.optionalNonNegativeUInt("offset", offsetRaw)
        val offsetInvalid = offsetParsed.invalidParameter
        if (offsetInvalid != null) {
            return listQueryParamError(response, sidCookie, offsetInvalid)
        }
        SessionCookies.echoSession(response, sidCookie, sessionService)
        val limit = limitParsed.value ?: Int.MAX_VALUE
        val offset = offsetParsed.value ?: 0
        val reviews = eventReviewService.listReviewsForEvent(eventId, limit, offset)
        return ResponseEntity.ok(ReviewListResponse(reviews = reviews, count = reviews.size))
    }

    @PatchMapping("/events/{event_id}/reviews/{review_id}")
    fun updateReview(
        @PathVariable("event_id") eventId: String,
        @PathVariable("review_id") reviewIdRaw: String,
        @RequestBody req: UpdateReviewRequest,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val sid = sessionService.resolveSession(sidCookie)
        val userId = sid?.let(sessionService::getUserId)
        if (sid == null || userId == null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        }
        val event = eventRepository.findById(eventId).orElse(null)
        if (event == null) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Event not found"))
        }
        val reviewId = try {
            UUID.fromString(reviewIdRaw.trim())
        } catch (_: IllegalArgumentException) {
            touchSession(response, sid)
            return ResponseEntity.badRequest()
                .body(MessageResponse("""invalid "review_id" field"""))
        }
        val badField = validateUpdateReview(req)
        if (badField != null) {
            touchSession(response, sid)
            return ResponseEntity.badRequest()
                .body(MessageResponse("""invalid "$badField" field"""))
        }
        val updated = eventReviewService.tryUpdateReview(
            eventId = eventId,
            reviewId = reviewId,
            userId = userId,
            comment = req.comment?.trim(),
            rating = req.rating,
            eventTitle = event.title,
        )
        if (!updated) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Event not found"))
        }
        touchSession(response, sid)
        return ResponseEntity.noContent().build<Void>()
    }

    private fun listQueryParamError(
        response: HttpServletResponse,
        sidCookie: String?,
        fieldName: String,
    ): ResponseEntity<MessageResponse> {
        SessionCookies.echoSession(response, sidCookie, sessionService)
        return ResponseEntity.badRequest()
            .body(MessageResponse("""invalid "$fieldName" field"""))
    }

    private fun validateCreateReview(req: CreateReviewRequest): String? {
        if (req.comment.isNullOrBlank()) return "comment"
        if (req.comment.trim().length > 300) return "comment"
        if (req.rating == null) return "rating"
        if (req.rating !in 1..5) return "rating"
        return null
    }

    private fun validateUpdateReview(req: UpdateReviewRequest): String? {
        if (req.comment == null && req.rating == null) return "rating"
        req.rating?.let {
            if (it !in 1..5) return "rating"
        }
        req.comment?.let {
            if (it.trim().length > 300) return "comment"
        }
        return null
    }

    private fun touchSession(response: HttpServletResponse, sid: String) {
        sessionService.touchSession(sid)
        SessionCookies.setSession(response, sid, sessionService.getTtl().toInt())
    }
}
