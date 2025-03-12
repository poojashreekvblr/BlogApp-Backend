package com.example.blog.controller

import com.example.blog.Dto.PostDto
import com.example.blog.Dto.PostResponse
import com.example.blog.service.PostService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/posts")
class PostController(private val postService: PostService) {

    @GetMapping
    suspend fun getAllPosts(): List<PostResponse> {
        return postService.getAllPosts()
    }

    @GetMapping("/{username}")
    suspend fun getAllUserPosts(@PathVariable username:String) : List<PostResponse>{
        return postService.getAllUserPosts(username)
    }

    @PostMapping
    suspend fun createPost(@RequestBody post: PostDto, @RequestHeader("Authorization") token: String): ResponseEntity<String> {
        val cleanedToken = token.replace("Bearer ", "")
        return postService.createPost(post, cleanedToken)
    }

    @PutMapping("/update/{id}")
    suspend fun updatePost(@PathVariable id: Long, @RequestBody post: PostDto, @RequestHeader("Authorization") token: String): ResponseEntity<String> {
        val cleanedToken = token.replace("Bearer ", "")
        return postService.updatePost(id, post, cleanedToken)
    }

    @DeleteMapping("/delete/{id}")
    suspend fun deletePost(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<String> {
        val cleanedToken = token.replace("Bearer ", "")
        return postService.deletePost(id, cleanedToken)
    }
}
