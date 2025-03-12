package com.example.blog

import com.example.blog.Dto.AuthenticationRequest
import com.example.blog.Dto.PostDto
import com.example.blog.Dto.PostResponse
import com.example.blog.model.User
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = ["spring.datasource.url=jdbc:h2:mem:testdb"])
class IntegrationTest {

    @LocalServerPort
    var port:Int = 0

    @Autowired
    lateinit var webTestClient: WebTestClient


    //   **/users/register**
    @Test
    fun `register a user successfully`(){
        println("Running on port ${port}")
        val user = User(username = "poojashree", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")
    }

    @Test
    fun `registering new user with existing username should return proper message`(){
        val user = User(username = "poojashree", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val user1 = User(username = "poojashree", password = "1234")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user1)
            .exchange()
            .expectStatus().is4xxClientError
            .expectBody()
            .jsonPath("$").isEqualTo("Username already exists, choose a different one")
    }

    @Test
    fun `login successfully`(){
        val user = User(username = "poojashree", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val authRequest = AuthenticationRequest("poojashree","123")

        webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
    }

    @Test
    fun `send a message if invalid token`(){
        val authRequest = AuthenticationRequest("pooja","1234")

        webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isUnauthorized
            .expectBody()
            .jsonPath("$.message").isEqualTo("Invalid username or password")
    }

    // **/posts getAllPosts**
    @Test
    fun `retrieve all posts`(){

        webTestClient.get().uri("/posts")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(PostResponse::class.java)
    }

    // **/posts/{username} getAllUserPosts**
    @Test
    @Order(3)
    fun `retrieve all user posts with valid token`() {

        val user = User(username = "pooja", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val authRequest = AuthenticationRequest("pooja", "123")
        val loginResponse = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken = loginResponse.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        println("Access Token: $accessToken")

        val username = "pooja"
        webTestClient.get().uri("/posts/$username")
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(PostResponse::class.java)
    }

    @Test
    fun `send empty list if invalid token`(){
        val invalidToken = "invalid_token"

        val username = "pooja"
        webTestClient.get().uri("/posts/$username")
            .header("Authorization", "Bearer $invalidToken")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `reject the request if no token provided`(){

        val username = "pooja"
        webTestClient.get().uri("/posts/$username")
            .exchange()
            .expectStatus().isForbidden
    }

    // **/posts createPost**
    @Test
    fun `successfully create a post with valid token and data`() {

        val user = User(username = "pooja", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val authRequest = AuthenticationRequest("pooja", "123")
        val loginResponse = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken = loginResponse.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        val postDto = PostDto("pooja","title", "content")

        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post created successfully")
    }

    @Test
    fun `fail to create a post with invalid token`(){

        val invalidToken = "invalid_token"
        val postDto = PostDto("pooja","title", "content")

        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $invalidToken")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `test for duplicate title`(){

        val user = User(username = "pooja", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val authRequest = AuthenticationRequest("pooja", "123")
        val loginResponse = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken = loginResponse.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        val postDto = PostDto("pooja","title", "content")

        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post created successfully")

        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$").isEqualTo("Title already present")
    }

    @Test
    fun `update the post successfully`(){

        val user = User(username = "pooja", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val authRequest = AuthenticationRequest("pooja", "123")
        val loginResponse = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken = loginResponse.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        val postDto = PostDto("pooja","title", "content")

        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post created successfully")

        val updatePost = PostDto("pooja","updated title","updated content")

        webTestClient.put().uri("/posts/update/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatePost)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post updated successfully")
    }

    @Test
    fun `post not found to update`(){
        val user = User(username = "pooja", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val authRequest = AuthenticationRequest("pooja", "123")
        val loginResponse = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken = loginResponse.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        val postDto = PostDto("pooja","title", "content")

        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post created successfully")

        val updatePost = PostDto("pooja","updated title","updated content")

        webTestClient.put().uri("/posts/update/2")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatePost)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isNotFound
            .expectBody()
            .jsonPath("$").isEqualTo("Post not found")
    }

    @Test
    fun `invalid token to update`(){
        val invalidToken = "invalid token"
        val updatePost = PostDto("pooja","title","content")
        webTestClient.put().uri("/posts/update/1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(updatePost)
            .header("Authorization", "Bearer $invalidToken")
            .exchange()
            .expectStatus().isForbidden
    }

    // **/posts/delete**
    @Test
    fun `delete a post successfully`(){

        val user = User(username = "pooja", password = "123")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$").isEqualTo("User registered successfully")

        val authRequest = AuthenticationRequest("pooja", "123")
        val loginResponse = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken = loginResponse.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        val postDto = PostDto("pooja","title", "content")

        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post created successfully")

        webTestClient.delete().uri("/posts/delete/1")
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post deleted successfully")
    }

    @Test
    fun `Post not found to delete`(){
          val user = User(username = "pooja", password = "123")

            webTestClient.post().uri("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$").isEqualTo("User registered successfully")

            val authRequest = AuthenticationRequest("pooja", "123")
            val loginResponse = webTestClient.post().uri("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty
                .returnResult()

            val accessToken = loginResponse.responseBody?.let {
                val objectMapper = ObjectMapper()
                val jsonNode = objectMapper.readTree(it)
                jsonNode["accessToken"].asText()
            }

            webTestClient.delete().uri("/posts/delete/1")
                .header("Authorization", "Bearer $accessToken")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$").isEqualTo("Post not found")
        }

    @Test
    fun `should return Unauthorized if not logged in`() {
        webTestClient.delete().uri("/posts/delete/1")
            .exchange()
            .expectStatus().isForbidden
            .expectBody()
    }

    @Test
    fun `should return Forbidden if user is not authorized to delete post`() {

        val user1 = User(username = "pooja", password = "123")
        val user2 = User(username = "alice", password = "abc")

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user1)
            .exchange()
            .expectStatus().isCreated

        webTestClient.post().uri("/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user2)
            .exchange()
            .expectStatus().isCreated

        val loginRequest = AuthenticationRequest("pooja", "123")
        val loginResponse = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken = loginResponse.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        val postDto = PostDto("pooja", "title", "content")
        webTestClient.post().uri("/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(postDto)
            .header("Authorization", "Bearer $accessToken")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$").isEqualTo("Post created successfully")


        val postId = 1L
        val loginRequest1 = AuthenticationRequest("alice", "abc")
        val loginResponse1 = webTestClient.post().uri("/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest1)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").isNotEmpty
            .returnResult()

        val accessToken1 = loginResponse1.responseBody?.let {
            val objectMapper = ObjectMapper()
            val jsonNode = objectMapper.readTree(it)
            jsonNode["accessToken"].asText()
        }

        webTestClient.delete().uri("/posts/delete/$postId")
            .header("Authorization", "Bearer $accessToken1")
            .exchange()
            .expectStatus().isForbidden
            .expectBody()
            .jsonPath("$").isEqualTo("You are not authorized to delete this post")
    }

    @Test
    fun `should return Unauthorized if token is expired or invalid`() {
        val invalidToken = "expired_or_invalid_token"
        webTestClient.delete().uri("/posts/delete/1")
            .header("Authorization", "Bearer $invalidToken")
            .exchange()
            .expectStatus().isForbidden
    }
}