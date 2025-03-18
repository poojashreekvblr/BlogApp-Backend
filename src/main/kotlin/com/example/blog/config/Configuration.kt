package com.example.blog.config

import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.DelegatingSecurityContextRepository
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository

@Configuration
class Configuration{
    @Bean
    fun securityContextRepository(): SecurityContextRepository {
        return DelegatingSecurityContextRepository(
            RequestAttributeSecurityContextRepository(),
            HttpSessionSecurityContextRepository()
        )
    }

    @PostConstruct
    fun init(){
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
    }
}