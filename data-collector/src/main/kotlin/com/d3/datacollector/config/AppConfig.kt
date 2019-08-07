/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.config

import com.d3.commons.config.RMQConfig
import com.d3.datacollector.service.RabbitMqService
import com.d3.datacollector.service.RabbitMqServiceImpl
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import jp.co.soramitsu.iroha.java.Utils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import java.net.URI

@Configuration
class AppConfig {

    @Bean
    @Lazy
    fun queryAPI(
        @Value("\${iroha.user.id}") accountId: String,
        @Value("\${iroha.user.publicKeyHex}") pubKey: String,
        @Value("\${iroha.user.privateKeyHex}") privKey: String,
        @Value("\${iroha.toriiAddress}") toriiAddress: String
    ) = QueryAPI(
        IrohaAPI(URI(toriiAddress)), accountId, Utils.parseHexKeypair(
            pubKey, privKey
        )
    )

    @Bean
    @Lazy
    fun rmqConfig(
        @Value("\${rmq.host}") rmqHost: String,
        @Value("\${rmq.port}") rmqPort: String,
        @Value("\${rmq.irohaExchange}") rmqExchange: String
    ) = object : RMQConfig {
        override val host = rmqHost
        override val port = rmqPort.toInt()
        override val irohaExchange = rmqExchange
    }

    @Bean
    fun rabbitService(
        @Value("\${rmq.host}") rmqHost: String,
        @Value("\${rmq.port}") rmqPort: String
    ): RabbitMqService {
        return RabbitMqServiceImpl(rmqHost, rmqPort, dataCollectorExchange, transferBillingUdateRoutingKey)
    }

    companion object {
        private const val dataCollectorExchange = "data-collector"
        private const val outRoutingKeyPrefix = "d3.$dataCollectorExchange"
        private const val transferBillingUdateRoutingKey = "$outRoutingKeyPrefix.transfer-billing.update"
        const val queueName = "DC_QUEUE"
    }
}
