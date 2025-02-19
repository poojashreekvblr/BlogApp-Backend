package com.example.blog.Dto

import java.time.LocalDateTime

data class PostResponse(
    val postId:Long?,
    val username:String,
    val title:String,
    val content:String,
    val createdAt:LocalDateTime
)
