package com.d3.datacollector.config

import com.d3.datacollector.rabbitmq.Receiver
import com.d3.datacollector.service.RabbitMqService
import com.d3.datacollector.service.RabbitMqServiceImpl
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@ConditionalOnProperty(value = ["app.rabbitmq.enable"], havingValue = "true", matchIfMissing = true)
@Configuration
class RabbitConfig {

    val queueName = "data-collector-queue"
    val outRoutingKeyPrefix = "d3.data-collector"
    val transaferBillingUdateRoutingKey = "$outRoutingKeyPrefix.transfer-billing.update"
    val dataCollectorExchange = "data-collector"

    @Bean
    fun queue(): Queue {
        return Queue(queueName, false)
    }

    @Bean
    fun exchange(): TopicExchange {
        return TopicExchange(dataCollectorExchange)
    }

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(queueName)
    }

    @Bean
    fun container(
        connectionFactory: ConnectionFactory,
        listenerAdapter: MessageListenerAdapter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = connectionFactory
        container.setQueueNames(queueName)
        container.setMessageListener(listenerAdapter)
        return container
    }

    @Bean
    fun receiver(): Receiver {
        return Receiver()
    }

    @Bean
    fun listenerAdapter(receiver: Receiver): MessageListenerAdapter {
        return MessageListenerAdapter(receiver, "receiveMessage")
    }

    @Bean
    fun rabbitService(): RabbitMqService {
        return RabbitMqServiceImpl()
    }

}
