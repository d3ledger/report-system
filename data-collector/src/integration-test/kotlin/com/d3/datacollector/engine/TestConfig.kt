/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.engine

import com.d3.datacollector.service.RabbitMqService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class TestConfig {

    @Bean
    @Primary
    fun rabbitServiceMock(): RabbitMqService {
        return RabbitServiceMock()
    }
}
