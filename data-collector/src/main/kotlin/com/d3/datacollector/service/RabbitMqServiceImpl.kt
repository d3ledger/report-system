/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.service

import com.d3.datacollector.config.RabbitConfig
import com.d3.datacollector.model.BillingMqDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

class RabbitMqServiceImpl : RabbitMqService {
    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate
    private val jsonMapper = object : ObjectMapper() {
        init {
            configure(SerializationFeature.INDENT_OUTPUT, true)
        }
    };
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
