package com.d3.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class ReportMain {

    public static void main(String[] args) {
        SpringApplication.run(ReportMain.class, args);
    }
}