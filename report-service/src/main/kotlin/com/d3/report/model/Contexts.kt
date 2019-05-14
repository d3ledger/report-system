/*
 *
 *  Copyright D3 Ledger, Inc. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 * /
 */
package com.d3.report.model

import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

class AccountCustodyContext(
    val assetsContexts: HashMap<String, AssetCustodyContext> = HashMap()
)

class AssetCustodyContext(
    var commulativeFeeAmount: BigDecimal = BigDecimal("0"),
    var lastTransferTimestamp:Long = 0,
    var lastAssetSum: BigDecimal = BigDecimal("0")
)

@Entity
class AccountAssetCustodyContext(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val accountId: String,
    val assetId: String,
    var commulativeFeeAmount: BigDecimal = BigDecimal("0"),
    var lastTransferTimestamp: Long = 0,
    var lastAssetSum: BigDecimal = BigDecimal("0")
)
