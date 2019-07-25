/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.engine

import com.d3.commons.config.RMQConfig
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

    @Bean
    @Primary
    fun rmqConfigLocal() = object : RMQConfig {
        override val host = "127.0.0.1"
        override val port = 5672
        override val irohaExchange = "iroha"
    }
}
