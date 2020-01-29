/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.model

import com.d3.datacollector.utils.toDcBigDecimal
import java.math.BigDecimal
import java.util.*

open class Conflictable(var errorCode: String? = null, var message: String? = null)

data class BillingResponse(
    val transfer: Map<String, Map<String, Set<Billing>>> = emptyMap(),
    val custody: Map<String, Map<String, Set<Billing>>> = emptyMap(),
    val accountCreation: Map<String, Map<String, Set<Billing>>> = emptyMap(),
    val exchange: Map<String, Map<String, Set<Billing>>> = emptyMap(),
    val withdrawal: Map<String, Map<String, Set<Billing>>> = emptyMap()
) : Conflictable()

data class SingleBillingResponse(
    val feeInfo: MutableSet<BillingInfoEntry> = mutableSetOf(),
    val assetPrecision: Int = 0
) : Conflictable() {
    constructor(
        billingMap: Map<String, Set<Billing>>,
        assetPrecision: Int
    ) : this(assetPrecision = assetPrecision) {
        feeInfo.addAll(billingMap.map { (description, billingSet) ->
            BillingInfoEntry(
                feeCode = description,
                feeEntries = billingSet,
                updated = billingSet.maxBy { it.updated }!!.updated,
                created = billingSet.minBy { it.created }!!.created
            )
        })
    }
}

data class BillingInfoEntry(
    val feeCode: String = "",
    val feeEntries: Set<Billing> = emptySet(),
    val created: Long = 0,
    val updated: Long = 0
)

data class BillingMqDto(
    val feeCode: String = "",
    val domainName: String = "",
    val billingType: Billing.BillingTypeEnum = Billing.BillingTypeEnum.TRANSFER,
    val asset: String = "",
    val destination: String = "",
    val feeType: Billing.FeeTypeEnum = Billing.FeeTypeEnum.FRACTION,
    val feeNature: Billing.FeeNatureEnum = Billing.FeeNatureEnum.SUBTRACT,
    val feeComputation: Billing.FeeComputationEnum = Billing.FeeComputationEnum.FEE,
    val feeAccount: String? = null,
    var feeFraction: BigDecimal = "0.0".toDcBigDecimal(),
    val minAmount: BigDecimal = "0".toDcBigDecimal(),
    val maxAmount: BigDecimal = "-1".toDcBigDecimal(),
    var minFee: BigDecimal = "0".toDcBigDecimal(),
    var maxFee: BigDecimal = "-1".toDcBigDecimal(),
    var created: Long = 0L,
    var updated: Long = 0L
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

data class IrohaDetailValueDTO(
    val assetId: String,
    val destination: String,
    val feeType: String,
    val feeFraction: String,
    val feeNature: String,
    val feeComputation: String,
    val feeAccount: String,
    val minAmount: String,
    val maxAmount: String,
    val minFee: String,
    val maxFee: String
)

fun IrohaDetailValueDTO.toBilling(
    billingType: Billing.BillingTypeEnum,
    feeCode: String,
    domain: String
) = Billing(
    feeDescription = feeCode,
    domainName = domain,
    billingType = billingType,
    asset = this.assetId,
    destination = destination,
    feeType = Billing.FeeTypeEnum.valueOf(feeType),
    feeFraction = feeFraction.toDcBigDecimal(),
    feeNature = Billing.FeeNatureEnum.valueOf(feeNature),
    feeComputation = Billing.FeeComputationEnum.valueOf(feeComputation),
    feeAccount = feeAccount,
    minAmount = minAmount.toDcBigDecimal(),
    maxAmount = maxAmount.toDcBigDecimal(),
    minFee = minFee.toDcBigDecimal(),
    maxFee = maxFee.toDcBigDecimal()
)
