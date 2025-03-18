package com.example.blog.controller

import com.example.blog.Dto.PostDto
import com.example.blog.Dto.PostResponse
import com.example.blog.service.PostService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
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
    suspend fun createPost(@RequestBody post: PostDto, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<String> {
        return postService.createPost(post, userDetails.username)
    }

    @PutMapping("/update/{id}")
    suspend fun updatePost(@PathVariable id: Long, @RequestBody post: PostDto, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<String> {
        return postService.updatePost(id,post, userDetails.username)
    }

    @DeleteMapping("/delete/{id}")
    suspend fun deletePost(@PathVariable id: Long, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<String> {
        return postService.deletePost(id,userDetails.username)
    }
}
