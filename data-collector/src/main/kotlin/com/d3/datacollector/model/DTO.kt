package com.d3.datacollector.model

import java.math.BigDecimal
import java.util.*

open class Conflictable(var errorCode: String? = null, var message: String? = null)

data class BillingResponse(
    val transferBilling:HashMap<String, HashMap<String, Billing>> = HashMap()
) : Conflictable()

data class SingleBillingResponse(
    val billing: Billing = Billing()
) : Conflictable()


data class BillingMqDto(
    val accountId: String = "",
    val billingType: Billing.BillingTypeEnum = Billing.BillingTypeEnum.TRANSFER,
    val asset:String = "",
    var feeFraction: BigDecimal = BigDecimal("0.015"),
    var updated: Date? = null
)
