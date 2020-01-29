/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.cache

import com.d3.datacollector.model.Billing
import com.d3.datacollector.service.DbService
import jp.co.soramitsu.iroha.java.detail.Const.assetIdDelimiter
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
    fun getTransferBilling(domain: String, asset: String): Map<String, Set<Billing>> =
        getBilling(transferFee, domain, asset)

    fun getCustodyBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(custodyFee)
    fun getCustodyBilling(domain: String, asset: String): Map<String, Set<Billing>> =
        getBilling(custodyFee, domain, asset)

    fun getAccountCreationBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(accountCreationFee)
    fun getAccountCreationBilling(domain: String, asset: String): Map<String, Set<Billing>> =
        getBilling(accountCreationFee, domain, asset)

    fun getExchangeBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(exchangeFee)
    fun getExchangeBilling(domain: String, asset: String): Map<String, Set<Billing>> =
        getBilling(exchangeFee, domain, asset)

    fun getWithdrawalBilling(): Map<String, Map<String, Set<Billing>>> = getBilling(withdrawalFee)
    fun getWithdrawalBilling(domain: String, asset: String): Map<String, Set<Billing>> =
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
        if (!billingMap[domain]!!.containsKey(billing.feeDescription)) {
            billingMap[domain]!![billing.feeDescription] = ConcurrentHashMap.newKeySet<Billing>()
        }
        billingMap[domain]!![billing.feeDescription]!!.remove(billing)
        billingMap[domain]!![billing.feeDescription]!!.add(billing)
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
    ): Map<String, Set<Billing>> {
        if (billingMap.contains(domain)) {
            val resultMap = mutableMapOf<String, Set<Billing>>()
            val domainMap = billingMap[domain]!!
            domainMap.keys.forEach { feeCode ->
                val billingSet = domainMap[feeCode]!!
                var toAdd = filterToAdd(billingSet, assetId)
                if (toAdd.isEmpty()) {
                    val anyDomainAsset =
                        "$ANY_ASSET_SYMBOL$assetIdDelimiter${assetId.split(assetIdDelimiter)[1]}"
                    toAdd = filterToAdd(billingSet, anyDomainAsset)
                    if (toAdd.isEmpty()) {
                        toAdd = filterToAdd(billingSet, ANY_ASSET_SYMBOL)
                    }
                }
                if (toAdd.isNotEmpty()) {
                    resultMap[feeCode] = toAdd
                }
            }
            if (resultMap.isNotEmpty()) {
                return resultMap
            }
        }
        throw IllegalStateException("No requested type billing found for: $domain, $assetId")
    }

    private fun filterToAdd(billingSet: Set<Billing>, assetId: String): Set<Billing> =
        billingSet.filter { billing -> assetId == billing.asset }.toSet()

    // Domain -> <Fee code/billing description -> Set<Billing>>
    private fun getNewBillingMap() = ConcurrentHashMap<String, MutableMap<String, MutableSet<Billing>>>()

    @PostConstruct
    fun init() {
        dbService.billingRepo.findAll().forEach {
            addBillingByType(it)
        }
    }

    companion object : KLogging() {
        const val ANY_ASSET_SYMBOL = "*"
    }
}
