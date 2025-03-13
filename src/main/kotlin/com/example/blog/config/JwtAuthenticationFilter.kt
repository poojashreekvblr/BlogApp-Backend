package com.example.blog.config

import com.example.blog.service.CustomUserDetailsService
import com.example.blog.service.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val userDetailsService: CustomUserDetailsService,
    private val tokenService: TokenService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader: String? = request.getHeader("Authorization")

        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            println("Authorization header missing or invalid")
            filterChain.doFilter(request, response)
            return
        }

        val jwtToken = authHeader.substringAfter("Bearer ")
        println("JWT Token extracted: $jwtToken")
        val email = tokenService.extractEmail(jwtToken)

        if (email != null && SecurityContextHolder.getContext().authentication == null) {
            println("Extracted username from token: $email")
            try {
                val foundUser = userDetailsService.loadUserByUsername(email)

                if (tokenService.isValid(jwtToken, foundUser.username)) {
                    val authorities = getAuthoritiesFromToken(jwtToken)
                    println("Authorities:$authorities")

                    val authToken = UsernamePasswordAuthenticationToken(foundUser, null, authorities)
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

                    println("Before setting authentication, SecurityContextHolder: ${SecurityContextHolder.getContext().authentication}")
                    SecurityContextHolder.getContext().authentication = authToken
                    println("After setting authentication, SecurityContextHolder: ${SecurityContextHolder.getContext().authentication}")
                    println("Authentication set for user: $email")
                } else {
                    println("Invalid or expired token for user: $email")
                    response.status = HttpServletResponse.SC_UNAUTHORIZED
                    return
                }
            } catch (e: Exception) {
                println("Error processing JWT authentication: $e")
                response.status = HttpServletResponse.SC_UNAUTHORIZED
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getAuthoritiesFromToken(token: String): Collection<SimpleGrantedAuthority> {
        val roles = tokenService.extractRoles(token)
        return roles.map { SimpleGrantedAuthority(it) }
    }
}

