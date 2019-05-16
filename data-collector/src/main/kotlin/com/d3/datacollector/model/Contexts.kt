/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.model

import java.math.BigDecimal
import javax.persistence.*

@Entity
class AccountAssetCustodyContext(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val accountId: String,
    val assetId: String,
    var commulativeFeeAmount: BigDecimal = BigDecimal("0"),
    var lastTransferTimestamp: Long? = null,
    var lastAssetSum: BigDecimal = BigDecimal("0")
)
