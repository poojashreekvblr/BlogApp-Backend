package com.example.blog.service

import com.example.blog.Dto.AuthenticationRequest
import com.example.blog.Dto.AuthenticationResponse
import com.example.blog.model.User
import com.example.blog.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationService: AuthenticationService
) {

    fun signUp(user: User): ResponseEntity<String> {
        val existingUser = userRepository.findByUsername(user.username)
        if (existingUser == null) {
            user.password = passwordEncoder.encode(user.password)
            userRepository.save(user)
            return ResponseEntity("User registered successfully", HttpStatus.CREATED)
        }
        return ResponseEntity("Username already exists, choose a different one", HttpStatus.CONFLICT)
    }

//
fun login(authRequest: AuthenticationRequest): ResponseEntity<Any> {
    try {
        val authResponse: AuthenticationResponse = authenticationService.authentication(authRequest)
        return ResponseEntity(authResponse, HttpStatus.OK)
    } catch (e: Exception) {
        return ResponseEntity(mapOf("message" to "Invalid username or password"), HttpStatus.UNAUTHORIZED)
    }
}
}
