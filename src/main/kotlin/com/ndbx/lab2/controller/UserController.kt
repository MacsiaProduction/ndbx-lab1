package com.ndbx.lab2.controller

import com.ndbx.lab2.dto.MessageResponse
import com.ndbx.lab2.dto.SignUpRequest
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.service.UserRegistrationService
import com.ndbx.lab2.service.UserRegistrationService.DuplicateUserException
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userRegistrationService: UserRegistrationService,
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
                val newSid = sessionService.createSessionWithUser(user.id!!.toHexString())
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

    private fun validateSignUp(req: SignUpRequest): String? {
        if (req.fullName.isNullOrBlank()) return "full_name"
        if (req.username.isNullOrBlank()) return "username"
        if (req.password.isNullOrBlank()) return "password"
        return null
    }
}
