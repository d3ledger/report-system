package jp.co.soramitsu.d3.datacollector.cache

import jp.co.soramitsu.d3.datacollector.model.Billing
import jp.co.soramitsu.d3.datacollector.service.DbService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CacheRepository {

    private val transferBilling = HashMap<String, HashMap<String, Billing>>()
    @Autowired
    private lateinit var dbService:DbService

    @Synchronized
    fun addTransferBilling(billing: Billing) {
        val domain = billing.accountId.substring(billing.accountId.indexOf('@') + 1)
        if (transferBilling.contains(domain)) {
            transferBilling.get(domain)?.put(billing.asset, billing)
        } else {
            transferBilling.put(domain, HashMap())
            transferBilling.get(domain)!!.put(billing.asset, billing)
        }
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
            if(transferBilling.get(domain)!!.contains(asset)) {
                return transferBilling.get(domain)!![asset]!!
            }
        }
        throw RuntimeException("No billing found for: $domain, $asset")
    }
}