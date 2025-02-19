package com.example.blog.service

import com.example.blog.Dto.PostDto
import com.example.blog.Dto.PostResponse
import com.example.blog.model.Post
import com.example.blog.repository.PostRepository
import com.example.blog.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Service
import java.util.*
import java.util.Collections.emptyList

@Service
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val tokenService: TokenService
) {

    fun getAllPosts(): List<PostResponse> {
        val posts = postRepository.findAll()
        return posts.map{post -> toPostResponse(post)}
    }

    fun getAllUserPosts(username: String): List<PostResponse> {
        val existingUser = userRepository.findByUsername(username)
        if (existingUser != null) {
            val posts = postRepository.findByUser(existingUser)
            return posts.map{post -> toPostResponse(post)}
        }
        return emptyList()
    }

    fun createPost(post: PostDto, token: String): ResponseEntity<String> {
        val username = extractUsernameFromToken(token)
        if (username.isNullOrBlank() || !tokenService.isValid(token, username)) {
            println("Invalid or expired token for username:$username")
            return ResponseEntity("Invalid or expired token", HttpStatus.UNAUTHORIZED)
        }

        val user = userRepository.findByUsername(username)
        if(user == null) {
            println("User not found in database:$username")
            return ResponseEntity("User not found", HttpStatus.NOT_FOUND)
        }
        val existingTitle = postRepository.findByTitle(post.title)
        if (existingTitle != null) {
            println("Duplicate title found:${post.title}")
            return ResponseEntity("Title already present", HttpStatus.BAD_REQUEST)
        }

        println("Creating post for user:${user.username}")
        val newPost = Post(
            title = post.title,
            content = post.content,
            user = user
        )
        postRepository.save(newPost)
        return ResponseEntity("Post created successfully", HttpStatus.OK)
    }

    fun updatePost(id: Long, post: PostDto, token: String): ResponseEntity<String> {
        val username = extractUsernameFromToken(token)
        if (username == null || !tokenService.isValid(token, username)) {
            return ResponseEntity("Invalid or expired token", HttpStatus.UNAUTHORIZED)
        }

        val existingPost = postRepository.findById(id)
        if (existingPost.isPresent) {
            val postToUpdate = existingPost.get()

            if (postToUpdate.user.username != username) {
                return ResponseEntity("You are not authorized to update this post", HttpStatus.FORBIDDEN)
            }

            val updatedPost = postToUpdate.copy(
                title = post.title,
                content = post.content
            )
            postRepository.save(updatedPost)
            return ResponseEntity("Post updated successfully", HttpStatus.OK)
        } else {
            return ResponseEntity("Post not found", HttpStatus.NOT_FOUND)
        }
    }

    fun deletePost(id: Long, token: String): ResponseEntity<String> {
        val username = extractUsernameFromToken(token)
        if (username == null || !tokenService.isValid(token, username)) {
            println("Invalid or expired token for user:$username")
            return ResponseEntity("Invalid or expired token", HttpStatus.UNAUTHORIZED)
        }

        val post = postRepository.findById(id)
        if (post.isPresent) {
            val postToDelete = post.get()

            println("Checking if user ${username} is authorized to delete post by ${postToDelete.user.username}")
            if (postToDelete.user.username != username) {
                println("User ${username} is not authorized to delete this post")
                return ResponseEntity("You are not authorized to delete this post", HttpStatus.FORBIDDEN)
            }

            postRepository.deleteById(id)
            println("Post deleted successfully by user ${username}")
            return ResponseEntity("Post deleted successfully", HttpStatus.OK)
        } else {
            println("Post not found with ID:$id")
            return ResponseEntity("Post not found", HttpStatus.NOT_FOUND)
        }
    }

    private fun extractUsernameFromToken(token: String): String? {
        return try {
            val email=tokenService.extractEmail(token)
            println("Extracted username:$email")
            email
        } catch (e: Exception) {
            println("Error extracting ysername:${e.message}")
            null
        }
    }

    private fun toPostResponse(post:Post) : PostResponse{
        return PostResponse(
            postId = post.postId,
            username = post.user.username,
            title = post.title,
            content = post.content,
            createdAt = post.createdAt
        )
    }
}
