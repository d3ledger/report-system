package com.d3.report.context

import java.math.BigDecimal
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
class AccountCustodyContext(
    val assetsContexts: HashMap<String, AssetCustodyContext> = HashMap()
)

class AssetCustodyContext(
    var commulativeFeeAmount: BigDecimal = BigDecimal("0"),
    var lastTransferTimestamp:Long? = null,
    var lastAssetSum: BigDecimal = BigDecimal("0")
)
