package com.ndbx.lab2.controller

import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.web.SessionCookies
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
            SessionCookies.setSession(response, existingSid, sessionService.getTtl().toInt())
            ResponseEntity.ok().build()
        } else {
            val newSid = sessionService.createUniqueSession()
            SessionCookies.setSession(response, newSid, sessionService.getTtl().toInt())
            ResponseEntity.status(201).build()
        }
    }

    companion object {
        const val SESSION_COOKIE = "X-Session-Id"
    }
}
