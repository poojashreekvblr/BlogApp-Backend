package com.example.blog.controller

import com.example.blog.Dto.PostDto
import com.example.blog.Dto.PostResponse
import com.example.blog.service.PostService
import com.example.blog.service.TokenService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
class PostController(private val postService: PostService,private val tokenService: TokenService) {

    @GetMapping
    suspend fun getAllPosts(): List<PostResponse> {
        return postService.getAllPosts()
    }

    @GetMapping("/{username}")
    suspend fun getAllUserPosts(@PathVariable username:String) : List<PostResponse>{
        return postService.getAllUserPosts(username)
    }

    @PostMapping
    suspend fun createPost(@RequestBody post: PostDto, @RequestHeader("Authorization") authHeader: String): ResponseEntity<String> {
        val token = authHeader.replace("Bearer ", "")
        val username = tokenService.extractEmail(token)
        return postService.createPost(post, username)
    }

    @PutMapping("/update/{id}")
    suspend fun updatePost(@PathVariable id: Long, @RequestBody post: PostDto, @RequestHeader("Authorization") authHeader: String): ResponseEntity<String> {
        val token = authHeader.replace("Bearer ", "")
        val username = tokenService.extractEmail(token)
        return postService.updatePost(id,post, username)
    }

    @DeleteMapping("/delete/{id}")
    suspend fun deletePost(@PathVariable id: Long, @RequestHeader("Authorization") authHeader: String): ResponseEntity<String> {
            val token = authHeader.replace("Bearer ", "")
            val username = tokenService.extractEmail(token)
            return postService.deletePost(id, username)
    }
}
