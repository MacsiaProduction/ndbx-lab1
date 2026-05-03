package com.ndbx.lab2.controller

import com.ndbx.lab2.document.EventDocument
import com.ndbx.lab2.document.EventLocation
import com.ndbx.lab2.dto.CreateEventRequest
import com.ndbx.lab2.dto.CreateEventResponse
import com.ndbx.lab2.dto.EventListResponse
import com.ndbx.lab2.dto.MessageResponse
import com.ndbx.lab2.dto.UpdateEventRequest
import com.ndbx.lab2.repository.EventRepository
import com.ndbx.lab2.service.EventCommandService
import com.ndbx.lab2.service.EventQueryService
import com.ndbx.lab2.service.EventReactionService
import com.ndbx.lab2.service.EventUpdateCommand
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.support.RequestSupport
import com.ndbx.lab2.support.SearchRequestSupport.parseEventSearchCriteria
import com.ndbx.lab2.support.toJson
import com.ndbx.lab2.support.wantsReactions
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.springframework.dao.DuplicateKeyException
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@RestController
class EventController(
    private val eventRepository: EventRepository,
    private val eventCommandService: EventCommandService,
    private val eventQueryService: EventQueryService,
    private val eventReactionService: EventReactionService,
    private val sessionService: SessionService,
) {
    @PostMapping("/events")
    fun create(
        @RequestBody req: CreateEventRequest,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Any> {
        val sid = sessionService.resolveSession(sidCookie)
        val userId = sid?.let(sessionService::getUserId)
        if (sid == null || userId == null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val badField = validateCreateEvent(req)
        if (badField != null) {
            touchSession(response, sid)
            return ResponseEntity.badRequest()
                .body(MessageResponse("""invalid "$badField" field"""))
        }
        val createdAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val title = req.title!!.trim()
        if (eventRepository.existsByTitle(title)) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MessageResponse("event already exists"))
        }
        val address = req.address!!.trim()
        val startedAt = req.startedAt!!.trim()
        val finishedAt = req.finishedAt!!.trim()
        val doc = EventDocument(
            title = title,
            description = req.description?.trim()?.takeIf(String::isNotEmpty),
            location = EventLocation(address = address),
            createdAt = createdAt,
            createdBy = userId,
            startedAt = startedAt,
            finishedAt = finishedAt,
        )
        return try {
            val saved = eventRepository.save(doc)
            touchSession(response, sid)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateEventResponse(id = saved.id!!))
        } catch (_: DuplicateKeyException) {
            touchSession(response, sid)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MessageResponse("event already exists"))
        }
    }

    @GetMapping("/events")
    fun list(
        @RequestParam(name = "id", required = false) idRaw: String?,
        @RequestParam(name = "title", required = false) title: String?,
        @RequestParam(name = "category", required = false) categoryRaw: String?,
        @RequestParam(name = "price_from", required = false) priceFromRaw: String?,
        @RequestParam(name = "price_to", required = false) priceToRaw: String?,
        @RequestParam(name = "address", required = false) addressRaw: String?,
        @RequestParam(name = "city", required = false) cityRaw: String?,
        @RequestParam(name = "date_from", required = false) dateFromRaw: String?,
        @RequestParam(name = "date_to", required = false) dateToRaw: String?,
        @RequestParam(name = "user_id", required = false) userIdRaw: String?,
        @RequestParam(name = "user", required = false) userRaw: String?,
        @RequestParam(name = "limit", required = false) limitRaw: String?,
        @RequestParam(name = "offset", required = false) offsetRaw: String?,
        @RequestParam(name = "include", required = false) include: String?,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val parsed = parseEventSearchCriteria(
            idRaw = idRaw,
            titleRaw = title,
            categoryRaw = categoryRaw,
            priceFromRaw = priceFromRaw,
            priceToRaw = priceToRaw,
            addressRaw = addressRaw,
            cityRaw = cityRaw,
            dateFromRaw = dateFromRaw,
            dateToRaw = dateToRaw,
            userIdRaw = userIdRaw,
            userRaw = userRaw,
            limitRaw = limitRaw,
            offsetRaw = offsetRaw,
        )
        parsed.invalidField?.let {
            return listQueryParamError(response, sidCookie, it)
        }
        SessionCookies.echoSession(response, sidCookie, sessionService)
        val events = eventQueryService.findFiltered(parsed.criteria!!)
        val items = if (wantsReactions(include)) {
            val countsByTitle = events.map { it.title }.distinct().associateWith { eventTitle ->
                eventReactionService.getReactionsForTitle(eventTitle)
            }
            events.map { it.toJson(countsByTitle.getValue(it.title)) }
        } else {
            events.map(EventDocument::toJson)
        }
        return ResponseEntity.ok(EventListResponse(events = items, count = items.size))
    }

    @GetMapping("/events/{id}")
    fun getOne(
        @PathVariable id: String,
        @RequestParam(name = "include", required = false) include: String?,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        SessionCookies.echoSession(response, sidCookie, sessionService)
        val event = eventRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Not found"))
        val reactions = if (wantsReactions(include)) {
            eventReactionService.getReactionsForTitle(event.title)
        } else {
            null
        }
        return ResponseEntity.ok(event.toJson(reactions))
    }

    @PostMapping("/events/{id}/like")
    fun like(
        @PathVariable id: String,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val sid = sessionService.resolveSession(sidCookie)
        val userId = sid?.let(sessionService::getUserId)
        if (sid == null || userId == null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        }
        val event = eventRepository.findById(id).orElse(null)
        if (event == null) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Event not found"))
        }
        eventReactionService.likeEvent(id, userId, event.title)
        touchSession(response, sid)
        return ResponseEntity.noContent().build<Void>()
    }

    @PostMapping("/events/{id}/dislike")
    fun dislike(
        @PathVariable id: String,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val sid = sessionService.resolveSession(sidCookie)
        val userId = sid?.let(sessionService::getUserId)
        if (sid == null || userId == null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        }
        val event = eventRepository.findById(id).orElse(null)
        if (event == null) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Event not found"))
        }
        eventReactionService.dislikeEvent(id, userId, event.title)
        touchSession(response, sid)
        return ResponseEntity.noContent().build<Void>()
    }

    @PatchMapping("/events/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody req: UpdateEventRequest,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val sid = sessionService.resolveSession(sidCookie)
        val userId = sid?.let(sessionService::getUserId)
        if (sid == null || userId == null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        }
        val badField = validateUpdateEvent(req)
        if (badField != null) {
            touchSession(response, sid)
            return ResponseEntity.badRequest()
                .body(MessageResponse("""invalid "$badField" field"""))
        }
        val event = eventRepository.findById(id).orElse(null)
        if (event == null || event.createdBy != userId) {
            touchSession(response, sid)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Not found. Be sure that event exists and you are the organizer"))
        }
        eventCommandService.updateEvent(
            eventId = id,
            organizerId = userId,
            command = EventUpdateCommand(
                category = req.category?.trim(),
                price = req.price,
                city = req.city?.trim(),
            ),
        )
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

    private fun validateCreateEvent(req: CreateEventRequest): String? {
        if (req.title.isNullOrBlank()) return "title"
        if (req.address.isNullOrBlank()) return "address"
        if (req.startedAt.isNullOrBlank()) return "started_at"
        if (req.finishedAt.isNullOrBlank()) return "finished_at"
        val started = RequestSupport.parseRfc3339(req.startedAt) ?: return "started_at"
        val finished = RequestSupport.parseRfc3339(req.finishedAt) ?: return "finished_at"
        if (!finished.isAfter(started)) return "finished_at"
        return null
    }

    private fun validateUpdateEvent(req: UpdateEventRequest): String? {
        req.category?.let {
            val category = it.trim()
            if (category.isEmpty() || com.ndbx.lab2.support.EventCategory.from(category) == null) {
                return "category"
            }
        }
        req.price?.let {
            if (it < 0) return "price"
        }
        return null
    }

    private fun touchSession(response: HttpServletResponse, sid: String) {
        sessionService.touchSession(sid)
        SessionCookies.setSession(response, sid, sessionService.getTtl().toInt())
    }
}
