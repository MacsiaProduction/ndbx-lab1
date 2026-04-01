package com.ndbx.lab2.controller

import com.ndbx.lab2.dto.LoginRequest
import com.ndbx.lab2.dto.MessageResponse
import com.ndbx.lab2.repository.UserRepository
import com.ndbx.lab2.service.SessionService
import com.ndbx.lab2.web.SessionCookies
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sessionService: SessionService,
) {

    @PostMapping("/login")
    fun login(
        @RequestBody req: LoginRequest,
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<*> {
        val effectiveSessionId = sessionService.resolveSession(sidCookie)
        val loginError = validateLogin(req)
        if (loginError != null) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.badRequest()
                .body(MessageResponse(loginError))
        }

        val username = req.username!!.trim()
        val password = req.password!!
        val user = userRepository.findByUsername(username)
        if (user == null || !passwordEncoder.matches(password, user.passwordHash)) {
            SessionCookies.refreshSession(response, sidCookie, sessionService)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MessageResponse("invalid credentials"))
        }
        val userIdHex = user.id!!.toHexString()
        if (effectiveSessionId != null) {
            if (!sessionService.bindUserToSession(effectiveSessionId, userIdHex)) {
                val sid = sessionService.createSessionWithUser(userIdHex)
                SessionCookies.setSession(response, sid, sessionService.getTtl().toInt())
            } else {
                SessionCookies.setSession(response, effectiveSessionId, sessionService.getTtl().toInt())
            }
        } else {
            val sid = sessionService.createSessionWithUser(userIdHex)
            SessionCookies.setSession(response, sid, sessionService.getTtl().toInt())
        }
        return ResponseEntity.noContent().build<Void>()
    }

    private fun validateLogin(req: LoginRequest): String? {
        if (req.username.isNullOrBlank()) return """invalid "username" field"""
        if (req.password.isNullOrBlank()) return """invalid "password" field"""
        return null
    }

    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = SessionController.SESSION_COOKIE, required = false) sidCookie: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val sid = sidCookie?.takeIf { sessionService.isValidSid(it) }
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        if (!sessionService.sessionExists(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Void>()
        }
        sessionService.deleteSession(sid)
        SessionCookies.clearSession(response, sid)
        return ResponseEntity.noContent().build<Void>()
    }
}
