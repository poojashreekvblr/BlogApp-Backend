package com.example.blog.config

import com.example.blog.repository.UserRepository
import com.example.blog.service.CustomUserDetailsService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfiguration(
    private val userRepository: UserRepository,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun userDetailsService(userRepository: UserRepository) : UserDetailsService =
        CustomUserDetailsService(userRepository)

    @Bean
    fun passwordEncoder() : PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationProvider(userRepository: UserRepository) : AuthenticationProvider =
        DaoAuthenticationProvider()
            .apply {
                setUserDetailsService(userDetailsService(userRepository))
                setPasswordEncoder(passwordEncoder())
            }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration) : AuthenticationManager =
        config.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): DefaultSecurityFilterChain {
        return http
            .csrf().disable()
            .authorizeHttpRequests {
                it.requestMatchers("/users/register", "/users/login","/posts").permitAll()
                it.requestMatchers("/h2-console/**").permitAll()
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/posts/**").hasRole("USER")
                it.requestMatchers(HttpMethod.POST, "/posts/**").hasRole("USER")
                it.requestMatchers(HttpMethod.PUT, "/posts/**").hasRole("USER")
                it.requestMatchers(HttpMethod.DELETE, "/posts/delete/**").hasRole("USER")
                it.anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider(userRepository))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:3000")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("Authorization", "Content-Type")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)

        return CorsFilter(source)
    }
}
