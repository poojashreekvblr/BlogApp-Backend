package com.example.blog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.blog"])
class BlogApplication

fun main(args: Array<String>) {
	runApplication<BlogApplication>(*args)
}
