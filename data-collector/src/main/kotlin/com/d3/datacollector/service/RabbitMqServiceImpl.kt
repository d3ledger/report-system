/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.service

import com.d3.datacollector.model.BillingMqDto
import com.d3.datacollector.utils.gson
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.impl.DefaultExceptionHandler
import mu.KLogging
import kotlin.system.exitProcess

class RabbitMqServiceImpl(
    rmqHost: String,
    rmqPort: String,
    private val exchange: String,
    private val routingKey: String
) : RabbitMqService {

    private val connectionFactory = ConnectionFactory()

    init {
        connectionFactory.exceptionHandler = object : DefaultExceptionHandler() {
            override fun handleConnectionRecoveryException(conn: Connection, exception: Throwable) {
                logger.error("Billing updates RMQ connection error", exception)
                exitProcess(1)
            }

            override fun handleUnexpectedConnectionDriverException(
                conn: Connection,
                exception: Throwable
            ) {
                logger.error("Billing updates RMQ connection error", exception)
                exitProcess(1)
            }
        }
        connectionFactory.host = rmqHost
        connectionFactory.port = rmqPort.toInt()
        connectionFactory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true)
            }
        }
    }

    override fun sendBillingUpdate(update: BillingMqDto) {
        connectionFactory.newConnection().use { connection ->
            connection.createChannel().use { channel ->
                val json = gson.toJson(update)
                logger.info("Sending mq billing update: $json")
                channel.basicPublish(
                    exchange,
                    routingKey,
                    MessageProperties.MINIMAL_PERSISTENT_BASIC,
                    json.toByteArray()
                )
            }
        }
    }

    companion object : KLogging()
}
