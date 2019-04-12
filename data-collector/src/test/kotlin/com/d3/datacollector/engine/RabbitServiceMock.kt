package com.d3.datacollector.engine

import com.d3.datacollector.model.BillingMqDto
import com.d3.datacollector.service.RabbitMqService
import mu.KLogging

class RabbitServiceMock : RabbitMqService {

    private val logger = KLogging().logger

    override fun sendBillingUpdate(update: BillingMqDto) {
        logger.warn("Execution of RabbitMqMockService method. Use only for tests")
    }
}
