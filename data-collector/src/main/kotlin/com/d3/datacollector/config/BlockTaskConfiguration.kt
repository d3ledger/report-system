/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.config

import com.d3.commons.config.RMQConfig
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val queueName = "DC_QUEUE"

@Configuration
class BlockTaskConfiguration {

    @Value("\${rmq.host}")
    private lateinit var rmqHost: String
    @Value("\${rmq.port}")
    private lateinit var rmqPort: String
    @Value("\${rmq.irohaExchange}")
    private lateinit var rmqExchange: String

    private val rmqConfig =
        object : RMQConfig {
            override val host = rmqHost
            override val port = rmqPort.toInt()
            override val irohaExchange = rmqExchange
        }

    @Bean
    fun irohaChainListener() = ReliableIrohaChainListener(rmqConfig, queueName)
}
