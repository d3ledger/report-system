
@file:JvmName("CollectorMain")

package jp.co.soramitsu.d3.datacollector

import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class Application

    private val logger = KLogging().logger

    fun main(args: Array<String>) {
        val app = SpringApplication(Application::class.java)
        app.run(*args)
    }


