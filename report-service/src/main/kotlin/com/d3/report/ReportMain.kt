@file:JvmName("ReportMain")

package com.d3.report

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableSwagger2
class Report

fun main(args: Array<String>) {
    SpringApplication.run(Report::class.java, *args)
}
