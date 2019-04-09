@file:JvmName("CollectorMain")

package jp.co.soramitsu.d3.datacollector

import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import javax.servlet.annotation.WebServlet


@SpringBootApplication
open class Application

private val logger = KLogging().logger

fun main(args: Array<String>) {
    val app = SpringApplication(Application::class.java)
    app.run(*args)
}
