/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.service

import com.d3.datacollector.model.BillingMqDto

interface RabbitMqService {

    fun sendBillingUpdate(update: BillingMqDto)
}
