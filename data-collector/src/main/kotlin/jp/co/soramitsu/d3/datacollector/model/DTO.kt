package jp.co.soramitsu.d3.datacollector.model

import java.math.BigDecimal

open class Conflictable(var errorCode: String? = null, var message: String? = null)

data class BillingResponse(
    val transferBilling:HashMap<String, HashMap<String, BigDecimal>> = HashMap()
) : Conflictable()