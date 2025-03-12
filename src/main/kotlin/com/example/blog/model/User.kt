package com.example.blog.model

import jakarta.persistence.*

@Entity
@Table(name="users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, updatable = false)
    val username: String,

    @Column(nullable = false)
    var password: String,
)