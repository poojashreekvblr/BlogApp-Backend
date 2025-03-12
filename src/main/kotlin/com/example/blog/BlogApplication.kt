package com.example.blog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(scanBasePackages = ["com.example.blog"])
@ComponentScan(basePackages = ["com.example.blog"])
class BlogApplication

fun main(args: Array<String>) {
	runApplication<BlogApplication>(*args)
}
