/*
 *
 *  Copyright D3 Ledger, Inc. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 * /
 */

package com.d3.report.service

import com.d3.report.context.AssetCustodyContext
import com.d3.report.model.InvalidValue
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CustodyService {

    @Value("\${billing.custody.period}")
    private var custodyPeriod: Long = 86400000

    /**
     * @param assetCustodyContextForAccount - context of fee calculation for asset in account
     * @param blockCreationTime - time of new transfer transaction block commit
     * @param feeFraction - Fraction of money that should be sent as fee.
     */
    fun addFeePortion(
        assetCustodyContextForAccount: AssetCustodyContext,
        blockCreationTime: Long,
        feeFraction: BigDecimal
    ) {
        if(feeFraction.compareTo(BigDecimal.ZERO) < 1) {
            throw InvalidValue("Fee fraction is zero or negative: $feeFraction")
        }

        if(feeFraction.compareTo(BigDecimal.ONE) == 1) {
            throw InvalidValue("Fee fraction should be between 0 and 1, but now we have: $feeFraction")
        }

        val previous =
            BigDecimal(assetCustodyContextForAccount.lastTransferTimestamp.toString())
        val new =
            BigDecimal(blockCreationTime.toString())
        val length = new.minus(previous)
        val custodyMultiplier = length.divide(BigDecimal(custodyPeriod), 8, RoundingMode.HALF_UP)
        val periodFeeMultiplier = feeFraction.multiply(custodyMultiplier)
        val fee = periodFeeMultiplier.multiply(assetCustodyContextForAccount.lastAssetSum)
        assetCustodyContextForAccount
            .commulativeFeeAmount =
            assetCustodyContextForAccount.commulativeFeeAmount.add(fee)
    }
}
