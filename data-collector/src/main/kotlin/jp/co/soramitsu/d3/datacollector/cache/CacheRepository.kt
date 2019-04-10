package jp.co.soramitsu.d3.datacollector.cache

import jp.co.soramitsu.d3.datacollector.model.Billing
import jp.co.soramitsu.d3.datacollector.service.DbService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Component
class CacheRepository {


    private val transferBilling = HashMap<String, HashMap<String, Billing>>()
    @Autowired
    lateinit var dbService:DbService

    @Synchronized
    fun addTransferBilling(billing: Billing) {
        if (transferBilling.contains(billing.accountId)) {
            transferBilling.get(billing.accountId)?.put(billing.asset, billing)
        } else {
            transferBilling.put(billing.accountId, HashMap())
            transferBilling.get(billing.accountId)!!.put(billing.asset, billing)
        }
    }

    @Synchronized
    fun getTransferBilling(): HashMap<String, HashMap<String, Billing>> {
        val copy = HashMap<String, HashMap<String, Billing>>()
        copy.putAll(transferBilling)
        return copy
    }

    @Synchronized
    fun getTransferBilling(id: String, asset: String): Billing {
        if (transferBilling.contains(id)) {
            if(transferBilling.get(id)!!.contains(asset)) {
                return transferBilling.get(id)!![asset]!!
            }
        }
        throw RuntimeException()
    }
}