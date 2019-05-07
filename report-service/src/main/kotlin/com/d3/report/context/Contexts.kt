package com.d3.report.context

class AccountCustodyContext(
    val assetsContexts: HashMap<String, AssetCustodyContext> = HashMap()
)

class AssetCustodyContext(
    var commulativeFeeAmount: Int = 0,
    var lastTransferTimestamp:Long
)
