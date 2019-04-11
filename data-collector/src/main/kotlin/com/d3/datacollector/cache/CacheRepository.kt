package com.d3.datacollector.cache

import com.d3.datacollector.model.Billing
import com.d3.datacollector.service.DbService
import com.d3.datacollector.utils.getDomainFromAccountId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CacheRepository {

    private val transferBilling = HashMap<String, HashMap<String, Billing>>()

    @Autowired
    private lateinit var dbService: DbService

    @Synchronized
    fun addTransferBilling(billing: Billing) {
        val domain = getDomainFromAccountId(billing.accountId)
        if (!transferBilling.contains(domain)) {
            transferBilling[domain] = HashMap()
        }
        transferBilling[domain]!![billing.asset] = billing
    }

    @Synchronized
    fun getTransferBilling(): HashMap<String, HashMap<String, Billing>> {
        val copy = HashMap<String, HashMap<String, Billing>>()
        copy.putAll(transferBilling)
        return copy
    }

    @Synchronized
    fun getTransferBilling(domain: String, asset: String): Billing {
        if (transferBilling.contains(domain)) {
            if(transferBilling[domain]!!.contains(asset)) {
                return transferBilling[domain]!![asset]!!
            }
        }
        throw RuntimeException("No billing found for: $domain, $asset")
    }
}
