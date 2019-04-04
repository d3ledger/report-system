
@file:JvmName("CollectorMain")

package jp.co.soramitsu.d3.reportsystem.datacollector

import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["jp.co.soramitsu.bootstrap"])
class Application

    private val logger = KLogging().logger

    fun main(args: Array<String>) {
        val app = SpringApplication(Application::class.java)
        app.run(*args)
    }


