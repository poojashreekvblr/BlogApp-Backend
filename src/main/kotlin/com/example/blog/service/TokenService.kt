package com.example.blog.service

import com.example.blog.config.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.Date

@Service
class TokenService(
    jwtProperties: JwtProperties
) {
    private val secretKey = Keys.hmacShaKeyFor(
        jwtProperties.key.toByteArray()
    )

    fun generate(
        userDetails: UserDetails,
        expirationDate: Date,
    ): String {
        // Get roles from user details and convert to a comma-separated string
        val authorities = userDetails.authorities.map { it.authority }

        return Jwts.builder()
            .setSubject(userDetails.username)
            .claim("roles", authorities) // Add roles to the JWT token
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(expirationDate)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }

    fun extractEmail(token: String): String? =
        getAllClaims(token).subject

    fun extractRoles(token: String): List<String> {
        val rolesClaim = getAllClaims(token).get("roles", List::class.java)
        return rolesClaim as List<String>
    }

    fun isExpired(token: String): Boolean =
        getAllClaims(token).expiration.before(Date(System.currentTimeMillis()))

    fun isValid(token: String, username: String): Boolean {
        val email = extractEmail(token)
        return username == email && !isExpired(token)
    }

    fun getAllClaims(token: String): Claims {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JWT token", e)
        }
    }
}
