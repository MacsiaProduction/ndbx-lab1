package com.ndbx.lab2.controller

import com.ndbx.lab2.service.SessionService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionController(private val sessionService: SessionService) {

    @PostMapping("/session")
    fun session(
        @CookieValue(name = SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val existingSid = sidCookie
            ?.takeIf { sessionService.isValidSid(it) }
            ?.takeIf { sessionService.touchSession(it) }

        return if (existingSid != null) {
            addSessionCookie(response, existingSid, sessionService.getTtl())
            ResponseEntity.ok().build()
        } else {
            val newSid = sessionService.createUniqueSession()
            addSessionCookie(response, newSid, sessionService.getTtl())
            ResponseEntity.status(201).build()
        }
    }

    private fun addSessionCookie(response: HttpServletResponse, sid: String, ttl: Long) {
        val cookie = Cookie(SESSION_COOKIE, sid).apply {
            isHttpOnly = true
            path = "/"
            maxAge = ttl.toInt()
        }
        response.addCookie(cookie)
    }

    companion object {
        const val SESSION_COOKIE = "X-Session-Id"
    }
}
