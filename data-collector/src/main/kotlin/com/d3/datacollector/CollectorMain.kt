@file:JvmName("CollectorMain")
package com.d3.datacollector

import com.d3.datacollector.service.BlockTaskService
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableSwagger2
class Collector

fun main(args: Array<String>) {
    val applicationContext = SpringApplication.run(Collector::class.java)
    applicationContext.getBean(BlockTaskService::class.java).runService()
}
