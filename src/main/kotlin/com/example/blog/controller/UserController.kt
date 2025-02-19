package com.example.blog.controller

import com.example.blog.Dto.AuthenticationRequest
import com.example.blog.Dto.AuthenticationResponse
import com.example.blog.Dto.UserDto
import com.example.blog.model.User
import com.example.blog.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    fun register(@RequestBody user : User) : ResponseEntity<String> = userService.signUp(user)

    @PostMapping("/login")
    fun login(@RequestBody user:AuthenticationRequest) : ResponseEntity<Any> = userService.login(user)
}
