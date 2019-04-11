package com.d3.datacollector.service

import com.d3.datacollector.model.BillingMqDto

interface RabbitMqService {

    fun sendBillingUpdate(update: BillingMqDto)
}
