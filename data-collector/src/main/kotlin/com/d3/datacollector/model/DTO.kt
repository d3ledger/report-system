/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.model

import java.math.BigDecimal
import java.util.*

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
    val domain: String = "",
    val billingType: Billing.BillingTypeEnum = Billing.BillingTypeEnum.TRANSFER,
    val asset: String = "",
    var feeFraction: BigDecimal = BigDecimal("0.015"),
    var updated: Long = 0L,
    var created: Long = 0L
)

/**
 * [Conflictable] wrapper with a [Boolean] value
 */
class BooleanWrapper(
    val itIs: Boolean = false,
    errorCode: String? = null,
    errorMessage: String? = null
) : Conflictable(errorCode, errorMessage)

/**
 * [Conflictable] wrapper with an [Int] value
 */
class IntegerWrapper(
    val itIs: Int? = null,
    errorCode: String? = null,
    errorMessage: String? = null
) : Conflictable(errorCode, errorMessage)

/**
 * [Conflictable] wrapper with a [String] value
 */
class StringWrapper(
    val itIs: String? = null,
    errorCode: String? = null,
    errorMessage: String? = null
) : Conflictable(errorCode, errorMessage)

class AssetsResponse(
    val currencies: Map<String?, String?> = HashMap(),
    val securities: Map<String?, String?> = HashMap(),
    val utilityAssets: Map<String?, String?> = HashMap(),
    val privateAssets: Map<String?, String?> = HashMap(),
    errorCode: String? = null,
    errorMessage: String? = null
) : Conflictable(errorCode, errorMessage)
