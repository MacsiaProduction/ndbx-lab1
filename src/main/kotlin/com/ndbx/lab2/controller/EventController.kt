package com.ndbx.lab2.controller

import com.ndbx.lab2.document.EventDocument
import com.ndbx.lab2.document.EventLocation
import com.ndbx.lab2.dto.CreateEventRequest
import com.ndbx.lab2.dto.CreateEventResponse
import com.ndbx.lab2.dto.EventListItemJson
import com.ndbx.lab2.dto.EventListResponse
import com.ndbx.lab2.dto.EventLocationJson
import com.ndbx.lab2.dto.MessageResponse
import com.ndbx.lab2.repository.EventRepository
import com.ndbx.lab2.service.EventQueryService
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.support.RequestSupport
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@RestController
class EventController(
    private val eventRepository: EventRepository,
    private val eventQueryService: EventQueryService,
    private val sessionService: SessionService,
) {

    @PostMapping("/events")
    fun create(
        @RequestBody req: CreateEventRequest,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Any> {
        val sid = sessionService.resolveSession(sidCookie)
        if (sid == null || sessionService.getUserId(sid) == null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val userIdHex = sessionService.getUserId(sid)!!
        val badField = validateCreateEvent(req)
        if (badField != null) {
            sessionService.touchSession(sid)
            SessionCookies.setSession(response, sid, sessionService.getTtl().toInt())
            return ResponseEntity.badRequest()
                .body(MessageResponse("""invalid "$badField" field"""))
        }
        val createdAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val title = req.title!!.trim()
        val address = req.address!!.trim()
        val startedAt = req.startedAt!!.trim()
        val finishedAt = req.finishedAt!!.trim()
        val doc = EventDocument(
            title = title,
            description = req.description?.takeIf { it.isNotBlank() },
            location = EventLocation(address = address),
            createdAt = createdAt,
            createdBy = ObjectId(userIdHex),
            startedAt = startedAt,
            finishedAt = finishedAt,
        )
        return try {
            val saved = eventRepository.save(doc)
            sessionService.touchSession(sid)
            SessionCookies.setSession(response, sid, sessionService.getTtl().toInt())
            ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateEventResponse(id = saved.id!!.toHexString()))
        } catch (_: DuplicateKeyException) {
            sessionService.touchSession(sid)
            SessionCookies.setSession(response, sid, sessionService.getTtl().toInt())
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MessageResponse("event already exists"))
        }
    }

    @GetMapping("/events")
    fun list(
        @RequestParam(name = "title", required = false) title: String?,
        @RequestParam(name = "limit", required = false) limitRaw: String?,
        @RequestParam(name = "offset", required = false) offsetRaw: String?,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val limit = RequestSupport.optionalNonNegativeUInt("limit", limitRaw)
        limit.invalidParameter?.let {
            return listQueryParamError(response, sidCookie, it)
        }
        val offset = RequestSupport.optionalNonNegativeUInt("offset", offsetRaw)
        offset.invalidParameter?.let {
            return listQueryParamError(response, sidCookie, it)
        }
        val list = eventQueryService.findFiltered(title, limit.value, offset.value ?: 0)
        SessionCookies.echoSession(response, sidCookie, sessionService)
        val items = list.map { toJson(it) }
        return ResponseEntity.ok(EventListResponse(events = items, count = items.size))
    }

    private fun listQueryParamError(
        response: HttpServletResponse,
        sidCookie: String?,
        parameterName: String,
    ): ResponseEntity<MessageResponse> {
        SessionCookies.echoSession(response, sidCookie, sessionService)
        return ResponseEntity.badRequest()
            .body(MessageResponse("""invalid "$parameterName" parameter"""))
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

    private fun toJson(e: EventDocument) = EventListItemJson(
        id = e.id!!.toHexString(),
        title = e.title,
        description = e.description,
        location = EventLocationJson(address = e.location.address),
        createdAt = e.createdAt,
        createdBy = e.createdBy.toHexString(),
        startedAt = e.startedAt,
        finishedAt = e.finishedAt,
    )
}
