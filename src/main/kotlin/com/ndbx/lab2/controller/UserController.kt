package com.ndbx.lab2.controller

import com.ndbx.lab2.dto.EventListResponse
import com.ndbx.lab2.dto.MessageResponse
import com.ndbx.lab2.dto.SignUpRequest
import com.ndbx.lab2.dto.UserListResponse
import com.ndbx.lab2.repository.UserRepository
import com.ndbx.lab2.service.EventQueryService
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.service.UserQueryService
import com.ndbx.lab2.service.UserRegistrationService
import com.ndbx.lab2.service.UserRegistrationService.DuplicateUserException
import com.ndbx.lab2.support.SearchRequestSupport.parseEventSearchCriteria
import com.ndbx.lab2.support.SearchRequestSupport.parseUserSearchCriteria
import com.ndbx.lab2.support.toJson
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userRegistrationService: UserRegistrationService,
    private val userQueryService: UserQueryService,
    private val userRepository: UserRepository,
    private val eventQueryService: EventQueryService,
    private val sessionService: SessionService,
) {
    @PostMapping("/users")
    fun register(
        @RequestBody req: SignUpRequest,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val invalidField = validateSignUp(req)
        if (invalidField != null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity
                .badRequest()
                .body(MessageResponse("""invalid "$invalidField" field"""))
        }
        val fullName = req.fullName!!.trim()
        val username = req.username!!.trim()
        val password = req.password!!
        val result = userRegistrationService.register(fullName, username, password)
        return result.fold(
            onSuccess = { user ->
                val newSid = sessionService.createSessionWithUser(user.id!!)
                SessionCookies.setSession(response, newSid, sessionService.getTtl().toInt())
                ResponseEntity.status(HttpStatus.CREATED).build<Void>()
            },
            onFailure = { e ->
                when (e) {
                    is DuplicateUserException -> {
                        SessionCookies.refreshSession(response, sidCookie, sessionService)
                        ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(MessageResponse("user already exists"))
                    }
                    else -> throw e
                }
            },
        )
    }

    @GetMapping("/users")
    fun list(
        @RequestParam(name = "id", required = false) idRaw: String?,
        @RequestParam(name = "name", required = false) nameRaw: String?,
        @RequestParam(name = "limit", required = false) limitRaw: String?,
        @RequestParam(name = "offset", required = false) offsetRaw: String?,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val parsed = parseUserSearchCriteria(
            idRaw = idRaw,
            nameRaw = nameRaw,
            limitRaw = limitRaw,
            offsetRaw = offsetRaw,
        )
        parsed.invalidField?.let {
            return listQueryParamError(response, sidCookie, it)
        }
        SessionCookies.echoSession(response, sidCookie, sessionService)
        val items = userQueryService.findFiltered(parsed.criteria!!).map { it.toJson() }
        return ResponseEntity.ok(UserListResponse(users = items, count = items.size))
    }

    @GetMapping("/users/{id}")
    fun getOne(
        @PathVariable id: String,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        SessionCookies.echoSession(response, sidCookie, sessionService)
        val user = userRepository.findById(id).orElse(null)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("Not found"))
        return ResponseEntity.ok(user.toJson())
    }

    @GetMapping("/users/{id}/events")
    fun listEvents(
        @PathVariable id: String,
        @RequestParam(name = "id", required = false) eventIdRaw: String?,
        @RequestParam(name = "title", required = false) titleRaw: String?,
        @RequestParam(name = "category", required = false) categoryRaw: String?,
        @RequestParam(name = "price_from", required = false) priceFromRaw: String?,
        @RequestParam(name = "price_to", required = false) priceToRaw: String?,
        @RequestParam(name = "address", required = false) addressRaw: String?,
        @RequestParam(name = "city", required = false) cityRaw: String?,
        @RequestParam(name = "date_from", required = false) dateFromRaw: String?,
        @RequestParam(name = "date_to", required = false) dateToRaw: String?,
        @RequestParam(name = "user", required = false) userRaw: String?,
        @RequestParam(name = "limit", required = false) limitRaw: String?,
        @RequestParam(name = "offset", required = false) offsetRaw: String?,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        if (!userRepository.existsById(id)) {
            SessionCookies.echoSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MessageResponse("User not found"))
        }
        val parsed = parseEventSearchCriteria(
            idRaw = eventIdRaw,
            titleRaw = titleRaw,
            categoryRaw = categoryRaw,
            priceFromRaw = priceFromRaw,
            priceToRaw = priceToRaw,
            addressRaw = addressRaw,
            cityRaw = cityRaw,
            dateFromRaw = dateFromRaw,
            dateToRaw = dateToRaw,
            userRaw = userRaw,
            limitRaw = limitRaw,
            offsetRaw = offsetRaw,
            createdByUserIdOverride = id,
        )
        parsed.invalidField?.let {
            return listQueryParamError(response, sidCookie, it)
        }
        SessionCookies.echoSession(response, sidCookie, sessionService)
        val items = eventQueryService.findFiltered(parsed.criteria!!).map { it.toJson() }
        return ResponseEntity.ok(EventListResponse(events = items, count = items.size))
    }

    private fun validateSignUp(req: SignUpRequest): String? {
        if (req.fullName.isNullOrBlank()) return "full_name"
        if (req.username.isNullOrBlank()) return "username"
        if (req.password.isNullOrBlank()) return "password"
        return null
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
}
