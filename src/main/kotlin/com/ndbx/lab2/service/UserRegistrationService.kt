package com.ndbx.lab2.service

import com.ndbx.lab2.document.UserDocument
import com.ndbx.lab2.repository.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun register(fullName: String, username: String, password: String): Result<UserDocument> {
        if (userRepository.existsByUsername(username)) {
            return Result.failure(DuplicateUserException())
        }
        val doc = UserDocument(
            fullName = fullName,
            username = username,
            passwordHash = passwordEncoder.encode(password),
        )
        return try {
            Result.success(userRepository.save(doc))
        } catch (_: DuplicateKeyException) {
            Result.failure(DuplicateUserException())
        }
    }

    class DuplicateUserException : RuntimeException()
}
