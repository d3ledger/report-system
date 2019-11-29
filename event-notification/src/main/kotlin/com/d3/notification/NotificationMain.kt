@file:JvmName("NotificationMain")

package com.d3.notification

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Notification

fun main(args: Array<String>) {
    SpringApplication.run(Notification::class.java, *args)
}
