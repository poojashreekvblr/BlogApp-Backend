package com.example.blog.repository

import com.example.blog.model.Post
import com.example.blog.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<Post, Long>{
    fun findByUser(user: User) : List<Post>
    fun findByTitle(title:String) : Post?
}
