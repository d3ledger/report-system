package com.d3.datacollector.model

import java.math.BigDecimal
import java.util.*
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
open class Conflictable(var errorCode: String? = null, var message: String? = null)

data class BillingResponse(
    val transfer: HashMap<String, HashMap<String, Billing>> = HashMap(),
    val custody: HashMap<String, HashMap<String, Billing>> = HashMap(),
    val accountCreation: HashMap<String, HashMap<String, Billing>> = HashMap(),
    val exchange: HashMap<String, HashMap<String, Billing>> = HashMap(),
    val withdrawal: HashMap<String, HashMap<String, Billing>> = HashMap()
    ) : Conflictable()

data class SingleBillingResponse(
    val billing: Billing = Billing()
) : Conflictable()


data class BillingMqDto(
    val accountId: String = "",
    val billingType: Billing.BillingTypeEnum = Billing.BillingTypeEnum.TRANSFER,
    val asset: String = "",
    var feeFraction: BigDecimal = BigDecimal("0.015"),
    var updated: Long = 0L,
    var created: Long = 0L
)

class BooleanWrapper(
    val itIs: Boolean = false,
    errorCode: String? = null,
    errorMessage: String? = null
) : Conflictable(errorCode,errorMessage)

class IntegerWrapper(
    val itIs: Int? = null,
    errorCode: String? = null,
    errorMessage: String? = null
) : Conflictable(errorCode,errorMessage)
