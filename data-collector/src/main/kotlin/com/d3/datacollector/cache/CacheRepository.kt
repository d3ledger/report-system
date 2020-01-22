/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.cache

import com.d3.datacollector.model.Billing
import com.d3.datacollector.service.DbService
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Component
class CacheRepository {
    private val transferFee = getNewBillingMap()
    private val custodyFee = getNewBillingMap()
    private val accountCreationFee = getNewBillingMap()
    private val exchangeFee = getNewBillingMap()
    private val withdrawalFee = getNewBillingMap()
    @Autowired
    private lateinit var dbService: DbService

    fun addBillingByType(billing: Billing) {
        when (billing.billingType) {
            Billing.BillingTypeEnum.TRANSFER -> addBilling(transferFee, billing)
            Billing.BillingTypeEnum.CUSTODY -> addBilling(custodyFee, billing)
            Billing.BillingTypeEnum.ACCOUNT_CREATION -> addBilling(accountCreationFee, billing)
            Billing.BillingTypeEnum.EXCHANGE -> addBilling(exchangeFee, billing)
            Billing.BillingTypeEnum.WITHDRAWAL -> addBilling(withdrawalFee, billing)
            else -> logger.error { "Can't match billing type ${billing.billingType} from database." }
        }
    }

    fun getTransferBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(transferFee)
    fun getTransferBilling(domain: String, asset: String): Set<Billing> =
        getBilling(transferFee, domain, asset)

    fun getCustodyBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(custodyFee)
    fun getCustodyBilling(domain: String, asset: String): Set<Billing> =
        getBilling(custodyFee, domain, asset)

    fun getAccountCreationBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(accountCreationFee)
    fun getAccountCreationBilling(domain: String, asset: String): Set<Billing> =
        getBilling(accountCreationFee, domain, asset)

    fun getExchangeBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(exchangeFee)
    fun getExchangeBilling(domain: String, asset: String): Set<Billing> =
        getBilling(exchangeFee, domain, asset)

    fun getWithdrawalBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(withdrawalFee)
    fun getWithdrawalBilling(domain: String, asset: String): Set<Billing> =
        getBilling(withdrawalFee, domain, asset)

    @Synchronized
    private fun addBilling(
        billingMap: MutableMap<String, MutableMap<String, MutableSet<Billing>>>,
        billing: Billing
    ) {
        val domain = billing.domainName
        if (!billingMap.containsKey(domain)) {
            billingMap[domain] = ConcurrentHashMap<String, MutableSet<Billing>>()
        }
        if (!billingMap[domain]!!.containsKey(billing.asset)) {
            billingMap[domain]!![billing.asset] = ConcurrentHashMap.newKeySet<Billing>()
        }
        billingMap[domain]!![billing.asset]!!.remove(billing)
        billingMap[domain]!![billing.asset]!!.add(billing)
    }

    private fun getBilling(billingMap: Map<String, Map<String, Set<Billing>>>): Map<String, Map<String, Set<Billing>>> {
        val copy = HashMap<String, Map<String, Set<Billing>>>()
        copy.putAll(billingMap)
        return copy
    }

    private fun getBilling(
        billingMap: Map<String, Map<String, Set<Billing>>>,
        domain: String,
        assetId: String
    ): Set<Billing> {
        if (billingMap.contains(domain)) {
            if (billingMap[domain]!!.contains(assetId)) {
                return billingMap[domain]!![assetId]!!
            }
        }
        throw IllegalStateException("No requested type billing found for: $domain, $assetId")
    }

    // Domain -> <Asset -> Set<Billing>>
    private fun getNewBillingMap() = ConcurrentHashMap<String, MutableMap<String, MutableSet<Billing>>>()

    @PostConstruct
    fun init() {
        dbService.billingRepo.findAll().forEach {
            addBillingByType(it)
        }
    }

    companion object : KLogging()
}
