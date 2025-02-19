package com.example.blog.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "posts")
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val postId : Long? = null,

    @Column(unique = true, nullable = false)
    var title : String,

    @Column(nullable = false)
    var content : String,

    @ManyToOne()
    @JoinColumn(name = "user_id", nullable = false)
    val user : User,

    @Column(nullable = false, updatable = false)
    var createdAt : LocalDateTime = LocalDateTime.now()
)