/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.engine

import com.d3.datacollector.model.BillingMqDto
import com.d3.datacollector.service.RabbitMqService
import mu.KLogging

class RabbitServiceMock : RabbitMqService {

    override fun sendBillingUpdate(update: BillingMqDto) {
        logger.warn("Execution of RabbitMqMockService method. Use only for tests")
    }

    companion object : KLogging()
}
