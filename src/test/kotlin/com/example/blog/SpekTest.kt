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
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

                assertEquals(foundUser?.username, "username")
                assertEquals(foundUser?.password, "password")
            }

            it("should return null if user is not found") {

                val foundUser = userRepository.findByUsername("nonexistent")

                assertNull(foundUser)
            }
        }

        context("when saving a user") {

            it("should successfully save a user") {

                val user = User(1, "username", "password")

                val savedUser = userRepository.save(user)

                assertEquals(savedUser, user)
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

                assertEquals(foundPosts.size,1)
                assertEquals(foundPosts[0].title,"title" )
            }
        }

        context("find all user posts"){

            it("return all user posts when user is present"){

                val user = User(1,"username","password")
                val posts = listOf(Post(1,"title","content",user, LocalDateTime.now()))

                whenever(postRepository.findByUser(user)).thenReturn(posts)

                val foundPosts = postRepository.findByUser(user)

                assertEquals(foundPosts,posts)
                assertEquals(foundPosts.size,1)
            }

            it("return empty list when user is not found"){

                val user = User(1,"username","password")

                whenever(postRepository.findByUser(user)).thenReturn(emptyList())

                val notFoundPosts = postRepository.findByUser(user)

                assertEquals(notFoundPosts.size,0)
            }
        }

        context("find the post with given title"){

            it("successfully return a post when title is present"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                whenever(postRepository.findByTitle("title")).thenReturn(post)

                val foundPosts = postRepository.findByTitle("title")

                assertEquals(foundPosts,post)
                assertEquals(foundPosts?.user,user)
            }

            it("return null if not post is present with a title"){

                whenever(postRepository.findByTitle("title")).thenReturn(null)

                val foundPosts = postRepository.findByTitle("title")

                assertNull(foundPosts)
            }
        }

        context("save the post"){

            it("successfully save the post"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                whenever(postRepository.save(post)).thenReturn(post)

                val savedPost = postRepository.save(post)

                assertEquals(savedPost,post)
            }

        }
        context("obtain post with given post id"){

            it("successfully return a post with given id"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                whenever(postRepository.findById(1)).thenReturn(Optional.of(post))

                val foundPost = postRepository.findById(1)

                assert(foundPost.isPresent)
            }

            it("Doesn't return anything if id is not there"){

                whenever(postRepository.findById(1)).thenReturn(Optional.empty())

                val foundPost = postRepository.findById(1)

                assert(foundPost.isEmpty)
            }
        }

        context("delete a post with post id"){

            it("successfully delete a post with given id"){

                val user = User(1,"username","password")
                val post = Post(1,"title","content",user, LocalDateTime.now())

                postRepository.save(post)

                postRepository.deleteById(1)

                assert(postRepository.findAll().isEmpty())
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

                val response: ResponseEntity<String> = userService.signUp(user)

                verify(userRepository).save(user)
                assertEquals(response.statusCode ,HttpStatus.CREATED)
                assertEquals(response.body,"User registered successfully")
                assertEquals(user.password ,encodedPassword)

            }

            it("send a message if username already present"){

                val user = User(id = 0, username = "pooja", password = "password")
                val existingUser = User(id = 1, username = "pooja", password = "oldPassword")

                whenever(userRepository.findByUsername(user.username)).thenReturn(existingUser)

                val response: ResponseEntity<String> = userService.signUp(user)

                verify(userRepository).findByUsername(user.username)
                assertEquals(response.statusCode , HttpStatus.CONFLICT)
                assertEquals(response.body ,"Username already exists, choose a different one")
            }
        }

        context("login"){

            it("login successfully"){

                val authrequest = AuthenticationRequest(username = "pooja", password = "password")
                val authreponse = AuthenticationResponse("token")

                whenever(authenticationService.authentication(authrequest)).thenReturn(authreponse)

                val response: ResponseEntity<Any> = userService.login(authrequest)

                assertEquals(HttpStatus.OK,response.statusCode)
                assertEquals(response.body,authreponse)
            }

            it("Invalid login credentails"){

                val authrequest = AuthenticationRequest("wrong-username","wrong-password")

                `when`(authenticationService.authentication(authrequest)).thenThrow(RuntimeException("Invalid credentials"))

                val response : ResponseEntity<Any> = userService.login(authrequest)
                val responseBody = response.body as Map<*,*>
                assertEquals("Invalid username or password",responseBody["message"])
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

                val response = postService.getAllPosts()

                assertEquals(1,posts.size)
            }

            it("retrieve empty list if no post is created"){

                whenever(postRepository.findAll()).thenReturn(emptyList())

                val response = postService.getAllPosts()

                assertEquals(0,response.size)
            }
        }

        context("get all user posts"){

            it("successfully retrieve all user posts"){

                val user = User(1,"username","password")
                val posts = listOf(Post(1,"java","description",user, LocalDateTime.now()))

                whenever(userRepository.findByUsername(user.username)).thenReturn(user)
                whenever(postRepository.findByUser(user)).thenReturn(posts)

                val response = postService.getAllUserPosts(user.username)

                assertEquals(1,response.size)
            }

            it("return empty list if not able to find the user"){

                whenever(userRepository.findByUsername("username")).thenReturn(null)

                val response = postService.getAllUserPosts("username")

                assertEquals(0,response.size)
            }
        }

        context("create a post"){

            it("successfully create a post"){

                val postdto = PostDto("username","java","decription")
                val token = "validToken"
                val user = User(1,"username","password")

                whenever(tokenService.isValid(token,postdto.username)).thenReturn(true)
                whenever(postService.extractUsernameFromToken("validToken")).thenReturn("username")
                whenever(userRepository.findByUsername(postdto.username)).thenReturn(user)
                whenever(postRepository.findByTitle(postdto.title)).thenReturn(null)
                whenever(postRepository.save(any(Post::class.java))).thenReturn(null)

                val response : ResponseEntity<String> = postService.createPost(postdto,token)

                assertEquals(HttpStatus.OK,response.statusCode)
                assertEquals("Post created successfully",response.body)
            }

            it("return message if invalid token"){

                val postdto = PostDto("username","java","decription")
                val token = "InvalidToken"

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username1")
                whenever(tokenService.isValid(token,"username1")).thenReturn(false)

                val response : ResponseEntity<String> = postService.createPost(postdto,token)

                assertEquals(HttpStatus.UNAUTHORIZED,response.statusCode)
                assertEquals("Invalid or expired token",response.body)
            }

            it("return message if user is not registered to craete post"){

                val postdto = PostDto("username","java","decription")
                val token = "validToken"

                whenever(tokenService.isValid(token,postdto.username)).thenReturn(true)
                whenever(postService.extractUsernameFromToken("validToken")).thenReturn("username")
                whenever(userRepository.findByUsername(postdto.username)).thenReturn(null)

                val response : ResponseEntity<String> = postService.createPost(postdto,token)

                assertEquals(HttpStatus.NOT_FOUND,response.statusCode)
                assertEquals("User not found",response.body)
            }

            it("return message if it is a duplicate title"){

                val postdto = PostDto("username","java","decription")
                val token = "validToken"
                val user = User(1,"username","password")
                val post = Post(1,"java","some content",user,LocalDateTime.now())

                whenever(tokenService.isValid(token,postdto.username)).thenReturn(true)
                whenever(postService.extractUsernameFromToken("validToken")).thenReturn("username")
                whenever(userRepository.findByUsername(postdto.username)).thenReturn(user)
                whenever(postRepository.findByTitle(postdto.title)).thenReturn(post)

                val response : ResponseEntity<String> = postService.createPost(postdto,token)

                assertEquals(HttpStatus.BAD_REQUEST,response.statusCode)
                assertEquals("Title already present",response.body)
            }
        }

        context("update a post"){

            it("successfully update the post"){

                val postDto = PostDto("username","java","description")
                val token = "validtoken"
                val user = User(1,"username","password")
                val post = Post(1,"prev title","prev content",user, LocalDateTime.now())

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token,"username")).thenReturn(true)
                whenever(postRepository.findById(1)).thenReturn(Optional.of(post))

                val response : ResponseEntity<String> = postService.updatePost(1L,postDto,token)

                assertEquals(HttpStatus.OK,response.statusCode)
                assertEquals("Post updated successfully",response.body)
            }

            it("return message if invalid token"){

                val postDto = PostDto("username","java","description")
                val token = "validtoken"

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token,"username")).thenReturn(false)

                val response : ResponseEntity<String> = postService.updatePost(1L,postDto,token)

                assertEquals(HttpStatus.UNAUTHORIZED,response.statusCode)
                assertEquals("Invalid or expired token",response.body)
            }

            it("return message if post with given id not found"){

                val postDto = PostDto("username","java","description")
                val token = "validtoken"

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token,"username")).thenReturn(true)
                whenever(postRepository.findById(1)).thenReturn(Optional.empty())

                val response : ResponseEntity<String> = postService.updatePost(1L,postDto,token)

                assertEquals(HttpStatus.NOT_FOUND,response.statusCode)
                assertEquals("Post not found",response.body)
            }

            it("return message if user not authorised to update the post"){

                val postDto = PostDto("username","java","description")
                val token = "validtoken"
                val user = User(1,"username1","password")
                val post = Post(1,"prev title","prev content",user, LocalDateTime.now())

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token,"username")).thenReturn(true)
                whenever(postRepository.findById(1)).thenReturn(Optional.of(post))

                val response : ResponseEntity<String> = postService.updatePost(1L,postDto,token)

                assertEquals(HttpStatus.FORBIDDEN,response.statusCode)
                assertEquals("You are not authorized to update this post",response.body)
            }
        }

        context("delete a post"){

            it("successfully delete a post"){

                val token = "validToken"
                val user = User(1, "username", "password")
                val post = Post(1L, "prev title", "prev content", user, LocalDateTime.now())

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token, "username")).thenReturn(true)
                whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))
                doNothing().whenever(postRepository).deleteById(1L)

                val response: ResponseEntity<String> = postService.deletePost(1L, token)

                assertEquals(HttpStatus.OK, response.statusCode)
                assertEquals("Post deleted successfully", response.body)
            }

            it("return message if invalid token"){

                val token = "validToken"
                val user = User(1, "username", "password")

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token, "username")).thenReturn(false)

                val response: ResponseEntity<String> = postService.deletePost(1L, token)

                assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
                assertEquals("Invalid or expired token", response.body)
            }

            it("return message if post with given id not found"){

                val token = "validToken"

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token, "username")).thenReturn(true)
                whenever(postRepository.findById(1L)).thenReturn(Optional.empty())

                val response: ResponseEntity<String> = postService.deletePost(1L, token)

                assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
                assertEquals("Post not found", response.body)
            }

            it("return message if user is not authorised to delete"){

                val token = "validToken"
                val user = User(1, "username1", "password")
                val post = Post(1L, "prev title", "prev content", user, LocalDateTime.now())

                whenever(postService.extractUsernameFromToken(token)).thenReturn("username")
                whenever(tokenService.isValid(token, "username")).thenReturn(true)
                whenever(postRepository.findById(1L)).thenReturn(Optional.of(post))

                val response: ResponseEntity<String> = postService.deletePost(1L, token)

                assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
                assertEquals("You are not authorized to delete this post", response.body)
            }
        }
    }

    describe("UserController"){

        lateinit var userRepository : UserRepository
        lateinit var authentication : AuthenticationService
        lateinit var passwordEncoder : PasswordEncoder
        lateinit var userService : UserService
        lateinit var mockMvc : MockMvc

        beforeEachTest {
            userRepository = mock()
            passwordEncoder = mock()
            authentication = mock()
            userService = UserService(userRepository, passwordEncoder, authentication)
            mockMvc = MockMvcBuilders
                .standaloneSetup(UserController(userService))
                .build()
        }

        context("testing /signup endpoint"){

            it("register the user successfully"){

                val user = User(1L, "username", "password")

                whenever(userRepository.findByUsername(user.username)).thenReturn(null)  // No existing user
                whenever(userRepository.save(user)).thenReturn(user)
                whenever(passwordEncoder.encode(user.password)).thenReturn("encodedPassword")

                mockMvc.perform(
                    post("/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
            {
            "username":"username",
            "password":"password"
            }
        """.trimIndent()))
                    .andExpect(status().isCreated)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("User registered successfully"))
            }

            it("return message as user already present"){

                val user = User(1L,"username","password")

                whenever(userRepository.findByUsername(user.username)).thenReturn(user)  // No existing user

                mockMvc.perform(
                    post("/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "username": "username",
                            "password": "password"
                        }
                    """.trimIndent()))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isConflict)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("Username already exists, choose a different one"))
            }
        }

        context("testing /login endpoint"){

            it("login successfully"){

                val user = AuthenticationRequest("username","password")
                val res = AuthenticationResponse("token")

                whenever(authentication.authentication(user)).thenReturn(res)

                mockMvc.perform(
                    post("/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"username\", \"password\":\"password\"}"))
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").value("token"))
            }

            it("unauthorized login"){

                val user = AuthenticationRequest("username", "password")

                whenever(authentication.authentication(user)).thenThrow(RuntimeException("Invalid username or password"))

                mockMvc.perform(
                    post("/users/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"username\", \"password\":\"password\"}"))
                    .andExpect(status().isUnauthorized)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"))
            }
        }
    }

    describe("PostController"){

        lateinit var postService : PostService
        lateinit var tokenService : TokenService
        lateinit var mockMvc : MockMvc

        beforeEachTest {
            postService = mock()
            tokenService = mock()
            mockMvc = MockMvcBuilders
                .standaloneSetup(PostController(postService))
                .build()
        }

        context("Testing /posts endpoint to get all posts"){

            it("successfully retrieve all posts"){

                val posts = listOf(PostResponse(1,"username","title","content", LocalDateTime.now()))

                whenever(postService.getAllPosts()).thenReturn(posts)

                mockMvc.perform(get("/posts"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.length()").value(1))
            }
        }

        context("Testing /posts/{username} endpoint to get user posts"){

            it("successfully retrieve all user posts"){

                val posts = listOf(PostResponse(1,"username","title","content", LocalDateTime.now()))

                whenever(postService.getAllUserPosts("username")).thenReturn(posts)

                mockMvc.perform(get("/posts/username"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.length()").value(1))
            }

            it("return empty list if not authorised"){

                whenever(postService.getAllUserPosts("username")).thenReturn(emptyList())

                mockMvc.perform(get("/posts/username"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.length()").value(0))
            }
        }

        context("testing /posts endpoint to create post"){

            it("successfully create a post"){

                val post = PostDto("username","title","content")

                val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

                whenever(tokenService.extractEmail(validToken)).thenReturn("username")

                whenever(postService.createPost(post,validToken)).thenReturn(ResponseEntity("Post created successfully", HttpStatus.OK))

                mockMvc.perform(
                    post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization","Bearer $validToken")
                        .content("""
                    {
                    "username":"username",
                    "title":"title",
                    "content":"content"
                    }
                    """.trimIndent())
                ).andExpect(status().isOk)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("Post created successfully"))
            }
        }

        context("testing /posts/update endpoint to update a post"){

            it("successfully update a post"){

                val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

                whenever(tokenService.extractEmail(validToken)).thenReturn("username")

                val post = PostDto("username","title","content")

                whenever(postService.updatePost(1,post,validToken)).thenReturn(ResponseEntity("Post updated successfully", HttpStatus.OK))

                mockMvc.perform(
                    put("/posts/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization","Bearer $validToken")
                        .content("""
                    {
                    "username":"username",
                    "title":"title",
                    "content":"content"
                    }
                    """.trimIndent())
                ).andExpect(status().isOk)
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("Post updated successfully"))
            }
        }

        context("testing /posts/delete endpoint to delete a post"){

            it("successfully delete a post"){

                val validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

                whenever(tokenService.extractEmail(validToken)).thenReturn("username")

                whenever(postService.deletePost(1, validToken)).thenReturn(ResponseEntity("Post deleted successfully", HttpStatus.OK))

                mockMvc.perform(
                    delete("/posts/delete/1")
                        .header("Authorization", "Bearer $validToken")
                )
                    .andExpect(status().isOk)
                    .andExpect(content().string("Post deleted successfully"))
            }
        }
    }
})