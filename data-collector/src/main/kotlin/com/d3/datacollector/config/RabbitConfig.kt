package com.d3.datacollector.config
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
import com.d3.datacollector.service.RabbitMqService
import com.d3.datacollector.service.RabbitMqServiceImpl
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.rabbit.core.RabbitTemplate

@ConditionalOnProperty(value = ["app.rabbitmq.enable"], havingValue = "true", matchIfMissing = true)
@Configuration
class RabbitConfig {
    val outRoutingKeyPrefix = "d3.data-collector"
    val transaferBillingUdateRoutingKey = "$outRoutingKeyPrefix.transfer-billing.update"
    val dataCollectorExchange = "data-collector"

    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(dataCollectorExchange)
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = producerJackson2MessageConverter()
        return rabbitTemplate
    }

    @Bean
    fun producerJackson2MessageConverter(): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitService(): RabbitMqService {
        return RabbitMqServiceImpl()
    }
}
