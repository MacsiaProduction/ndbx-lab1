package com.ndbx.lab2.controller

import com.ndbx.lab2.controller.SessionController.Companion.SESSION_COOKIE
import com.ndbx.lab2.service.SessionService
import jakarta.servlet.http.Cookie
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
        response: HttpServletResponse
    ): ResponseEntity<Map<String, String>> {
        if (sidCookie != null) {
            val cookie = Cookie(SESSION_COOKIE, sidCookie).apply {
                isHttpOnly = true
                path = "/"
                maxAge = sessionService.getTtl().toInt()
            }
            response.addCookie(cookie)
        }
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}
