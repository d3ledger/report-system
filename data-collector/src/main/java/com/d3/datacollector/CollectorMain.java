package com.d3.datacollector;

import com.d3.datacollector.cache.CacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class CollectorMain {

    public static void main(String[] args) {
        SpringApplication.run(CollectorMain.class, args);
    }
}
