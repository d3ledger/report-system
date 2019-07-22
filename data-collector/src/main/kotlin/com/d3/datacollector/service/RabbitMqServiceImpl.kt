/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.service

import com.d3.datacollector.config.RabbitConfig
import com.d3.datacollector.model.BillingMqDto
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

class RabbitMqServiceImpl : RabbitMqService {
    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate
    @Autowired
    private lateinit var rabbitConfig: RabbitConfig

    override fun sendBillingUpdate(update: BillingMqDto) {
        rabbitTemplate.convertAndSend(
            rabbitConfig.dataCollectorExchange,
            rabbitConfig.transaferBillingUdateRoutingKey,
            update
        )
    }
}
