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
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val userDetailsService: CustomUserDetailsService,
    private val tokenService: TokenService,
    private val securityContextRepository: SecurityContextRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val authHeader: String? = request.getHeader("Authorization")

            SecurityContextHolder.clearContext()

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
                    val foundUser = userDetailsService.loadUserByUsername(email)

                    if (tokenService.isValid(jwtToken, foundUser.username)) {
                        val authorities = getAuthoritiesFromToken(jwtToken)
                        println("Authorities:$authorities")

                        val authToken = UsernamePasswordAuthenticationToken(foundUser, null, authorities)
                        authToken.details = WebAuthenticationDetailsSource().buildDetails(request)

                        val context = SecurityContextHolder.createEmptyContext()
                        context.authentication = authToken
                        SecurityContextHolder.setContext(context)

                        securityContextRepository.saveContext(context,request,response)
                        println("Authentication set for user:$email")
                    } else {
                        println("Invalid or expired token for user: $email")
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Invalid token")
                        return
                    }
            }
            filterChain.doFilter(request, response)
        }
        catch (e:Exception){
            println("Authentication error:${e.message}")
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Authentication Failed")
            return
        }
    }

    private fun getAuthoritiesFromToken(token: String): Collection<SimpleGrantedAuthority> {
        val roles = tokenService.extractRoles(token)
        println("Roles extracted from JWT: $roles")
        return roles.map { SimpleGrantedAuthority(it) }
    }
}

