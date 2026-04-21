package com.ndbx.lab2.controller

import com.ndbx.lab2.controller.SessionController.Companion.SESSION_COOKIE
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController(private val sessionService: SessionService) {

    @GetMapping("/health")
    fun health(
        @CookieValue(name = SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Map<String, String>> {
        SessionCookies.echoSession(response, sidCookie, sessionService)
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}
