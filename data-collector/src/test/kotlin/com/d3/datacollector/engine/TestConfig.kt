/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
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
    fun rabbitService(): RabbitMqService {
        return RabbitServiceMock()
    }
}
