package com.d3.report.context

class AccountCustodyContext(
    val assetContexts: HashMap<String, AssetCustodyContext> = HashMap()
)

class AssetCustodyContext(
    var commulativeFeeAmount: Int = 0,
    var lastTransferTimestamp:Long,
    var
)
