package com.example.blog.service

import com.example.blog.repository.UserRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

typealias ApplicationUser = com.example.blog.model.User

@Service
class CustomUserDetailsService(
    private  val userRepository: UserRepository
) :UserDetailsService{
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")
        return user.mapToUserDetails()
    }

    private fun ApplicationUser.mapToUserDetails():UserDetails =
        User.builder()
            .username(this.username)
            .password(this.password)
            .authorities(getAuthoritiesForUser(this))
            .build()

    private fun getAuthoritiesForUser(user:ApplicationUser):Collection<GrantedAuthority>{
        return listOf(SimpleGrantedAuthority("ROLE_USER"))
    }
}