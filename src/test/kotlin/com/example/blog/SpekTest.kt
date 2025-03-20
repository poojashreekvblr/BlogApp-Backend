package com.example.blog

import com.example.blog.Dto.AuthenticationRequest
import com.example.blog.Dto.AuthenticationResponse
import com.example.blog.Dto.PostDto
import com.example.blog.Dto.PostResponse
import com.example.blog.controller.PostController
import com.example.blog.controller.UserController
import com.example.blog.model.Post
import com.example.blog.model.User
import com.example.blog.repository.PostRepository
import com.example.blog.repository.UserRepository
import com.example.blog.service.AuthenticationService
import com.example.blog.service.PostService
import com.example.blog.service.TokenService
import com.example.blog.service.UserService
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

@AutoConfigureWebTestClient
@RunWith(JUnitPlatform::class)
class SpekTest : Spek({

    describe("UserRepository") {
        lateinit var userRepository: UserRepository
        beforeEachTest {
            userRepository = mock()

            val user = User(1, "username", "password")

            whenever(userRepository.findByUsername("username")).thenReturn(user)
            whenever(userRepository.findByUsername("nonexistent")).thenReturn(null)
            whenever(userRepository.save(user)).thenReturn(user)
        }



        context("when finding a user by username") {

            it("should return user by username") {

                val foundUser = userRepository.findByUsername("username")

                foundUser?.username shouldBe "username"
                foundUser?.password shouldBe "password"
//                assertEquals(foundUser?.username, "username")
//                assertEquals(foundUser?.password, "password")
            }

            it("should return null if user is not found") {

                val foundUser = userRepository.findByUsername("nonexistent")

                foundUser shouldBe null
//                assertNull(foundUser)
            }
        }

        context("when saving a user") {

            it("should successfully save a user") {

                val user = User(1, "username", "password")

                val savedUser = userRepository.save(user)

                savedUser shouldBe user
//                assertEquals(savedUser, user)
            }
        }
    }

    describe("PostRepository"){
        lateinit var postRepository : PostRepository

        beforeEachTest {
            postRepository = mock()
        }

        context("find all posts"){

            it("return all posts"){

                val user = User(1,"username","password")
                val posts = listOf(Post(1,"title","content",user, LocalDateTime.now()))

                whenever(postRepository.findAll()).thenReturn(posts)

                val foundPosts = postRepository.findAll()

                foundPosts.size shouldBe 1
                foundPosts[0].title shouldBe "title"
//                assertEquals(foundPosts.size,1)
//                assertEquals(foundPosts[0].title,"title" )
            }
        }

        context("find all user posts"){

            it("return all user posts when user is present"){

                val user = User(1,"username","password")
                val posts = listOf(Post(1,"title","content",user, LocalDateTime.now()))

                whenever(postRepository.findByUser(user)).thenReturn(posts)

                val foundPosts = postRepository.findByUser(user)

                foundPosts shouldBe posts
                foundPosts.size shouldBe 1
//                assertEquals(foundPosts,posts)
//                assertEquals(foundPosts.size,1)
            }

            it("return empty list when user is not found"){

                val user = User(1,"username","password")

                whenever(postRepository.findByUser(user)).thenReturn(emptyList())

                val notFoundPosts = postRepository.findByUser(user)

                notFoundPosts.size shouldBe 0
//                assertEquals(notFoundPosts.size,0)
            }
        }

        context("find the post with given title"){

            it("successfully return a post when title is present"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                whenever(postRepository.findByTitle("title")).thenReturn(post)

                val foundPosts = postRepository.findByTitle("title")

                foundPosts shouldBe post
                foundPosts?.user shouldBe user
//                assertEquals(foundPosts,post)
//                assertEquals(foundPosts?.user,user)
            }

            it("return null if not post is present with a title"){

                whenever(postRepository.findByTitle("title")).thenReturn(null)

                val foundPosts = postRepository.findByTitle("title")

                foundPosts shouldBe null
//                assertNull(foundPosts)
            }
        }

        context("save the post"){

            it("successfully save the post"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                whenever(postRepository.save(post)).thenReturn(post)

                val savedPost = postRepository.save(post)

                savedPost shouldBe post
//                assertEquals(savedPost,post)
            }

        }
        context("obtain post with given post id"){

            it("successfully return a post with given id"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                whenever(postRepository.findById(1)).thenReturn(Optional.of(post))

                val foundPost = postRepository.findById(1)

                foundPost shouldNotBe null
//                assert(foundPost.isPresent)
            }

            it("Doesn't return anything if id is not there"){

                whenever(postRepository.findById(1)).thenReturn(Optional.empty())

                val foundPost = postRepository.findById(1)

                foundPost shouldBe Optional.empty()
//                assert(foundPost.isEmpty)
            }
        }

        context("delete a post with post id"){

            it("successfully delete a post with given id"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                postRepository.save(post)

                postRepository.deleteById(1)

                postRepository.findAll() shouldBe emptyList()
//                assert(postRepository.findAll().isEmpty())
            }
        }
    }

    describe("UserService") {

        lateinit var userRepository: UserRepository
        lateinit var passwordEncoder: PasswordEncoder
        lateinit var authenticationService: AuthenticationService
        lateinit var userService: UserService

        beforeEachTest {
            userRepository = mock()
            passwordEncoder = mock()
            authenticationService = mock()
            userService = UserService(userRepository,passwordEncoder,authenticationService)
        }

        context("signup"){

            it("register user successfully"){

                val user = User(id = 0, username = "pooja", password = "password")
                whenever(userRepository.findByUsername(user.username)).thenReturn(null)
                val encodedPassword = "encodedPassword"
                whenever(passwordEncoder.encode(user.password)).thenReturn(encodedPassword)

                val response: ResponseEntity<String> = runBlocking{userService.signUp(user)}

                verify(userRepository).save(user)

                response.statusCode shouldBe HttpStatus.CREATED
                response.body shouldBe "User registered successfully"
                user.password shouldBe encodedPassword
//                assertEquals(response.statusCode ,HttpStatus.CREATED)
//                assertEquals(response.body,"User registered successfully")
//                assertEquals(user.password ,encodedPassword)

            }

            it("send a message if username already present"){

                val user = User(id = 0, username = "pooja", password = "password")
                val existingUser = User(id = 1, username = "pooja", password = "oldPassword")

                whenever(userRepository.findByUsername(user.username)).thenReturn(existingUser)

                val response: ResponseEntity<String> = runBlocking{userService.signUp(user)}

                verify(userRepository).findByUsername(user.username)

                response.statusCode shouldBe HttpStatus.CONFLICT
                response.body shouldBe "Username already exists, choose a different one"
//                assertEquals(response.statusCode , HttpStatus.CONFLICT)
//                assertEquals(response.body ,"Username already exists, choose a different one")
            }
        }

        context("login"){

            it("login successfully"){

                val authrequest = AuthenticationRequest(username = "pooja", password = "password")
                val authreponse = AuthenticationResponse("token")

                whenever(authenticationService.authentication(authrequest)).thenReturn(authreponse)

                val response: ResponseEntity<Any> = runBlocking{userService.login(authrequest)}

                response.statusCode shouldBe HttpStatus.OK
                response.body shouldBe authreponse
//                assertEquals(HttpStatus.OK,response.statusCode)
//                assertEquals(response.body,authreponse)
            }

            it("Invalid login credentails"){

                val authrequest = AuthenticationRequest("wrong-username","wrong-password")

                `when`(authenticationService.authentication(authrequest)).thenThrow(RuntimeException("Invalid credentials"))

                val response : ResponseEntity<Any> = runBlocking{userService.login(authrequest)}
                val responseBody = response.body as Map<*,*>

                responseBody["message"] shouldBe "Invalid username or password"
//                assertEquals("Invalid username or password",responseBody["message"])
            }
        }
    }

    describe("PostService"){

        lateinit var postRepository : PostRepository
        lateinit var userRepository : UserRepository
        lateinit var tokenService : TokenService
        lateinit var postService : PostService

        beforeEachTest {

            postRepository = mock()
            userRepository = mock()
            tokenService = mock()
            postService = PostService(postRepository,userRepository,tokenService)
        }

        context("get all posts"){

            it("successfully retrieve all posts"){

                val user = User(0,"pooja","password")
                val posts = listOf(Post(1,"java","description",user, LocalDateTime.now()))

                whenever(postRepository.findAll()).thenReturn(posts)

                val response = runBlocking{postService.getAllPosts()}

                posts.size shouldBe 1
//                assertEquals(1,posts.size)
            }

            it("retrieve empty list if no post is created"){

                whenever(postRepository.findAll()).thenReturn(emptyList())

                val response = runBlocking{postService.getAllPosts()}

                response.size shouldBe 0
//                assertEquals(0,response.size)
            }
        }

        context("get all user posts"){

            it("successfully retrieve all user posts"){

                val user = User(1,"username","password")
                val posts = listOf(Post(1,"java","description",user, LocalDateTime.now()))

                whenever(userRepository.findByUsername(user.username)).thenReturn(user)
                whenever(postRepository.findByUser(user)).thenReturn(posts)

                val response = runBlocking{postService.getAllUserPosts(user.username)}

                response.size shouldBe 1
//                assertEquals(1,response.size)
            }

            it("return empty list if not able to find the user"){

                whenever(userRepository.findByUsername("username")).thenReturn(null)

                val response = runBlocking{postService.getAllUserPosts("username")}

                response.size shouldBe 0
//                assertEquals(0,response.size)
            }
        }

        context("create a post"){

            it("successfully create a post"){

                val postdto = PostDto("username","java","decription")
                val user = User(1,"username","password")

                whenever(userRepository.findByUsername(postdto.username)).thenReturn(user)
                whenever(postRepository.findByTitle(postdto.title)).thenReturn(null)
                whenever(postRepository.save(any(Post::class.java))).thenReturn(null)

                val response : ResponseEntity<String> = runBlocking{postService.createPost(postdto,"username")}

                response.statusCode shouldBe HttpStatus.OK
                response.body shouldBe "Post created successfully"
//                assertEquals(HttpStatus.OK,response.statusCode)
//                assertEquals("Post created successfully",response.body)
            }

            it("return message if invalid token"){

                val postdto = PostDto("username","java","decription")

                val response : ResponseEntity<String> = runBlocking{postService.createPost(postdto,"")}

                response.statusCode shouldBe HttpStatus.UNAUTHORIZED
                response.body shouldBe "Invalid or expired token"
//                assertEquals(HttpStatus.UNAUTHORIZED,response.statusCode)
//                assertEquals("Invalid or expired token",response.body)
            }

            it("return message if user is not registered to create post"){

                val postdto = PostDto("username","java","decription")

                whenever(userRepository.findByUsername(postdto.username)).thenReturn(null)

                val response : ResponseEntity<String> = runBlocking{postService.createPost(postdto,"username")}

                response.statusCode shouldBe HttpStatus.NOT_FOUND
                response.body shouldBe "User not found"
//                assertEquals(HttpStatus.NOT_FOUND,response.statusCode)
//                assertEquals("User not found",response.body)
            }

            it("return message if it is a duplicate title"){

                val postdto = PostDto("username","java","decription")
                val user = User(1,"username","password")
                val post = Post(1,"java","some content",user,LocalDateTime.now())

                whenever(userRepository.findByUsername(postdto.username)).thenReturn(user)
                whenever(postRepository.findByTitle(postdto.title)).thenReturn(post)

                val response : ResponseEntity<String> = runBlocking{postService.createPost(postdto,"username")}

                response.statusCode shouldBe HttpStatus.BAD_REQUEST
                response.body shouldBe "Title already present"
//                assertEquals(HttpStatus.BAD_REQUEST,response.statusCode)
//                assertEquals("Title already present",response.body)
            }
        }

        context("update a post"){

            it("successfully update the post"){

                val postDto = PostDto("username","java","description")
                val user = User(1,"username","password")
                val post = Post(1,"prev title","prev content",user, LocalDateTime.now())

                whenever(postRepository.findById(1)).thenReturn(Optional.of(post))

                val response : ResponseEntity<String> = runBlocking{postService.updatePost(1L,postDto,"username")}

                response.statusCode shouldBe HttpStatus.OK
                response.body shouldBe "Post updated successfully"
//                assertEquals(HttpStatus.OK,response.statusCode)
//                assertEquals("Post updated successfully",response.body)
            }

            it("return message if invalid token"){

                val postDto = PostDto("username","java","description")

                val response : ResponseEntity<String> = runBlocking{postService.updatePost(1L,postDto,"")}

                response.statusCode shouldBe HttpStatus.UNAUTHORIZED
                response.body shouldBe "Invalid or expired token"
//                assertEquals(HttpStatus.UNAUTHORIZED,response.statusCode)
//                assertEquals("Invalid or expired token",response.body)
            }

            it("return message if post with given id not found"){

                val postDto = PostDto("username","java","description")

                whenever(postRepository.findById(1)).thenReturn(Optional.empty())

                val response : ResponseEntity<String> = runBlocking{postService.updatePost(1L,postDto,"username")}

                response.statusCode shouldBe HttpStatus.NOT_FOUND
                response.body shouldBe "Post not found"
//                assertEquals(HttpStatus.NOT_FOUND,response.statusCode)
//                assertEquals("Post not found",response.body)
            }

            it("return message if user not authorised to update the post"){

                val postDto = PostDto("username","java","description")
                val user = User(1,"username1","password")
                val post = Post(1,"prev title","prev content",user, LocalDateTime.now())

                whenever(postRepository.findById(1)).thenReturn(Optional.of(post))

                val response : ResponseEntity<String> = runBlocking{postService.updatePost(1L,postDto,"username")}

                response.statusCode shouldBe HttpStatus.FORBIDDEN
                response.body shouldBe "You are not authorized to update this post"
//                assertEquals(HttpStatus.FORBIDDEN,response.statusCode)
//                assertEquals("You are not authorized to update this post",response.body)
            }
        }

        context("delete a post"){

            it("successfully delete a post"){

                val user = User(1, "username", "password")
                val post = Post(1L, "prev title", "prev content", user, LocalDateTime.now())

                whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))
                doNothing().whenever(postRepository).deleteById(1L)

                val response: ResponseEntity<String> = runBlocking{postService.deletePost(1L, "username")}

                response.statusCode shouldBe HttpStatus.OK
                response.body shouldBe "Post deleted successfully"
//                assertEquals(HttpStatus.OK, response.statusCode)
//                assertEquals("Post deleted successfully", response.body)
            }

            it("return message if invalid token"){


                val user = User(1, "username", "password")

                val response: ResponseEntity<String> = runBlocking{postService.deletePost(1L, "")}

                response.statusCode shouldBe HttpStatus.UNAUTHORIZED
                response.body shouldBe "Invalid or expired token"
//                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
//                assertEquals("Invalid or expired token", response.body)
            }

            it("return message if post with given id not found"){

                whenever(postRepository.findById(1L)).thenReturn(Optional.empty())

                val response: ResponseEntity<String> = runBlocking{postService.deletePost(1L, "username")}

                response.statusCode shouldBe HttpStatus.NOT_FOUND
                response.body shouldBe "Post not found"
//                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
//                assertEquals("Post not found", response.body)
            }

            it("return message if user is not authorised to delete"){

                val user = User(1, "username1", "password")
                val post = Post(1L, "prev title", "prev content", user, LocalDateTime.now())

                whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

                val response: ResponseEntity<String> = runBlocking{postService.deletePost(1L, "username")}

                response.statusCode shouldBe HttpStatus.FORBIDDEN
                response.body shouldBe "You are not authorized to delete this post"
//                assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
//                assertEquals("You are not authorized to delete this post", response.body)
            }
        }
    }

    describe("UserController"){

        lateinit var webTestClient : WebTestClient
        lateinit var userRepository : UserRepository
        lateinit var authentication : AuthenticationService
        lateinit var passwordEncoder : PasswordEncoder
        lateinit var userService : UserService

        beforeEachTest {
            userRepository = mock()
            passwordEncoder = mock()
            authentication = mock()
            userService = UserService(userRepository, passwordEncoder, authentication)
            webTestClient = WebTestClient.bindToController(UserController(userService)).build()
        }

        context("testing /signup endpoint"){

            it("register the user successfully") {

                val user = User(1L,"username", "password")

                whenever(userRepository.findByUsername(user.username)).thenReturn(null)  // No existing user
                whenever(userRepository.save(user)).thenReturn(user)
                whenever(passwordEncoder.encode(user.password)).thenReturn("encodedPassword")

                webTestClient.post().uri("/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(user)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(String::class.java)
                    .isEqualTo("User registered successfully")
            }

            it("return message as user already present"){

                val user = User(1L,"username","password")

                whenever(userRepository.findByUsername(user.username)).thenReturn(user)  // No existing user

                webTestClient.post().uri("/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(user)
                    .exchange()
                    .expectStatus().is4xxClientError
                    .expectBody(String::class.java)
                    .isEqualTo("Username already exists, choose a different one")

            }
        }

        context("testing /login endpoint"){

            it("login successfully"){

                val user = AuthenticationRequest("username","password")
                val res = AuthenticationResponse("token")

                whenever(authentication.authentication(user)).thenReturn(res)

                webTestClient.post()
                    .uri("/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"username\":\"username\", \"password\":\"password\"}")
                    .exchange()
                    .expectStatus().isOk
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.accessToken").isEqualTo("token")

            }

            it("unauthorized login"){

                val user = AuthenticationRequest("username", "password")

                whenever(authentication.authentication(user)).thenThrow(RuntimeException("Invalid username or password"))

                webTestClient.post()
                    .uri("/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"username\":\"username\", \"password\":\"password\"}")
                    .exchange()
                    .expectStatus().isUnauthorized
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Invalid username or password")
            }
        }
    }

    describe("PostController"){

        lateinit var webTestClient : WebTestClient
        lateinit var postService : PostService
        lateinit var tokenService : TokenService
        lateinit var mockMvc : MockMvc

        beforeEachTest {
            postService = mock()
            tokenService = mock()
            webTestClient = WebTestClient.bindToController(PostController(postService,tokenService)).build()
        }

        context("Testing /posts endpoint to get all posts"){

            it("successfully retrieve all posts"){

                val posts = listOf(PostResponse(1,"username","title","content", LocalDateTime.now()))

                whenever(runBlocking{postService.getAllPosts()}).thenReturn(posts)

                webTestClient.get().uri("/posts")
                    .exchange()
                    .expectStatus().isOk
                    .expectBodyList(PostResponse::class.java)
                    .hasSize(1)
            }
        }

        context("Testing /posts/{username} endpoint to get user posts"){

            it("successfully retrieve all user posts"){

                val posts = listOf(PostResponse(1,"username","title","content", LocalDateTime.now()))

                whenever(runBlocking{postService.getAllUserPosts("username")}).thenReturn(posts)

                webTestClient.get().uri("/posts/username")
                    .exchange()
                    .expectStatus().isOk
                    .expectBodyList(PostResponse::class.java)
                    .hasSize(1)
            }

            it("return empty list if not authorised"){

                whenever(runBlocking{postService.getAllUserPosts("username")}).thenReturn(emptyList())

                webTestClient.get().uri("/posts/username")
                    .exchange()
                    .expectStatus().isOk
                    .expectBodyList(PostResponse::class.java)
                    .hasSize(0)
            }
        }

        /// resolve
        context("testing /posts endpoint to create post"){

            it("successfully create a post"){
                val post = PostDto("username","title","content")
                val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

                whenever(tokenService.extractEmail(validToken)).thenReturn("username")
                whenever(runBlocking
                {postService.createPost(post, "username")})
                    .thenReturn(ResponseEntity("Post created successfully", HttpStatus.OK))

                webTestClient.post().uri("/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer $validToken")
                    .bodyValue("""
                    {
                        "username":"username",
                        "title":"title",
                        "content":"content"
                    }
                """.trimIndent())
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(String::class.java)
                    .isEqualTo("Post created successfully")
            }
        }

        context("testing /posts/update endpoint to update a post"){
            it("successfully update a post"){
                val post = PostDto("username","title","content")
                val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

                whenever(tokenService.extractEmail(validToken)).thenReturn("username")
                whenever(runBlocking
                {postService.updatePost(1L,post,"username")})
                    .thenReturn(ResponseEntity("Post updated successfully", HttpStatus.OK))

                webTestClient.put().uri("/posts/update/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer $validToken")
                    .bodyValue("""
                    {
                        "username":"username",
                        "title":"title",
                        "content":"content"
                    }
                """.trimIndent())
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(String::class.java)
                    .isEqualTo("Post updated successfully")
            }
        }

        context("testing /posts/delete endpoint to delete a post"){

            it("successfully delete a post"){
                val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

                whenever(tokenService.extractEmail(validToken)).thenReturn("username")
                whenever(runBlocking
                { postService.deletePost(1L, "username") })
                    .thenReturn(ResponseEntity("Post deleted successfully", HttpStatus.OK))

                webTestClient.delete().uri("/posts/delete/1")
                    .header("Authorization", "Bearer $validToken")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(String::class.java)
                    .isEqualTo("Post deleted successfully")
            }
        }
    }
})