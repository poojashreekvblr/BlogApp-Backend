package com.example.blog.service

import com.example.blog.Dto.AuthenticationRequest
import com.example.blog.Dto.AuthenticationResponse
import com.example.blog.config.JwtProperties
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthenticationService(
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: CustomUserDetailsService,
    private val tokenService: TokenService,
    private val jwtProperties: JwtProperties
) {

    fun authentication(authrequest: AuthenticationRequest): AuthenticationResponse{
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(
                authrequest.username,
                authrequest.password
            )
        )

        val user = userDetailsService.loadUserByUsername(authrequest.username)

        val accessToken = tokenService.generate(
            userDetails = user,
            expirationDate = Date(System.currentTimeMillis() + jwtProperties.accessTokenExpiration)
        )

        return  AuthenticationResponse(
            accessToken=accessToken
        )

    }
}