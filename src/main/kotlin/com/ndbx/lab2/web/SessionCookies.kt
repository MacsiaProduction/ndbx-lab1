package com.ndbx.lab2.web

import com.ndbx.lab2.controller.SessionController.Companion.SESSION_COOKIE
import com.ndbx.lab2.service.SessionService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse

object SessionCookies {
    fun setSession(response: HttpServletResponse, sid: String, maxAgeSeconds: Int) {
        response.addCookie(cookie(sid, maxAgeSeconds))
    }

    fun clearSession(response: HttpServletResponse, sid: String) {
        response.addCookie(cookie(sid, 0))
    }

    fun refreshSession(response: HttpServletResponse, sidCookie: String?, sessionService: SessionService) {
        val sid = sessionService.resolveSession(sidCookie) ?: return
        if (sessionService.touchSession(sid)) setSession(response, sid, sessionService.getTtl().toInt())
    }

    fun echoSession(response: HttpServletResponse, sidCookie: String?, sessionService: SessionService) {
        val sid = sessionService.resolveSession(sidCookie) ?: return
        setSession(response, sid, sessionService.getTtl().toInt())
    }

    private fun cookie(sid: String, maxAgeSeconds: Int) =
        Cookie(SESSION_COOKIE, sid).apply {
            isHttpOnly = true
            path = "/"
            maxAge = maxAgeSeconds
        }
}
