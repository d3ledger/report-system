/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector;

import com.d3.datacollector.service.BlockTaskService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
public class CollectorMain {

    public static void main(String[] args) {
        final ConfigurableApplicationContext applicationContext = SpringApplication.run(CollectorMain.class, args);
        applicationContext.getBean(BlockTaskService.class).runService();
    }
}
