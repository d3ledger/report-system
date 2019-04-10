package jp.co.soramitsu.d3.datacollector.cache

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Component
class CacheRepository {


    private val transferBilling = HashMap<String, HashMap<String, BigDecimal>>()

    @Synchronized
    fun addTransferBilling(id: String, asset: String, fee: BigDecimal) {
        if (transferBilling.contains(id)) {
            transferBilling.get(id)?.put(asset, fee)
        } else {
            transferBilling.put(id, HashMap<String, BigDecimal>())
            transferBilling.get(id)!!.put(asset, fee)
        }
    }

    @Synchronized
    fun getTransferBilling(): HashMap<String, HashMap<String, BigDecimal>> {
        val copy = HashMap<String, HashMap<String, BigDecimal>>()
        copy.putAll(transferBilling)
        return copy
    }

    @Synchronized
    fun getTransferBilling(id: String, asset: String): BigDecimal {
        if (transferBilling.contains(id)) {
            if(transferBilling.get(id)!!.contains(asset)) {
                return transferBilling.get(id)!![asset]!!
            }
        }
        throw RuntimeException()
    }
}