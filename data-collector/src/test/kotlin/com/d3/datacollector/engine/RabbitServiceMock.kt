package com.d3.datacollector.engine

import com.d3.datacollector.model.BillingMqDto
import com.d3.datacollector.service.RabbitMqService
import mu.KLogging
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
class RabbitServiceMock : RabbitMqService {

    private val logger = KLogging().logger

    override fun sendBillingUpdate(update: BillingMqDto) {
        logger.warn("Execution of RabbitMqMockService method. Use only for tests")
    }
}
