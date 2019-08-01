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
import javax.annotation.PostConstruct

@Component
class CacheRepository {
    private val transferFee = HashMap<String, HashMap<String, Billing>>()
    private val custodyFee = HashMap<String, HashMap<String, Billing>>()
    private val accountCreationFee = HashMap<String, HashMap<String, Billing>>()
    private val exchangeFee = HashMap<String, HashMap<String, Billing>>()
    private val withdrawalFee = HashMap<String, HashMap<String, Billing>>()
    @Autowired
    private lateinit var dbService: DbService

    fun addFeeByType(billing: Billing) {
        when (billing.billingType) {
            Billing.BillingTypeEnum.TRANSFER -> addTransferFee(billing)
            Billing.BillingTypeEnum.CUSTODY -> addCustodyFee(billing)
            Billing.BillingTypeEnum.ACCOUNT_CREATION -> addAccountCreationFee(billing)
            Billing.BillingTypeEnum.EXCHANGE -> addExchangeFee(billing)
            Billing.BillingTypeEnum.WITHDRAWAL -> addWithdrawalFee(billing)
            else -> logger.error { "Can't match billing type ${billing.billingType} from database." }
        }
    }

    private fun addTransferFee(billing: Billing) = addBilling(transferFee, billing)
    fun getTransferFee(): HashMap<String, HashMap<String, Billing>> = getBilling(transferFee)
    fun getTransferFee(domain: String, asset: String): Billing =
        getBilling(transferFee, domain, asset, Billing.BillingTypeEnum.TRANSFER)

    private fun addCustodyFee(billing: Billing) = addBilling(custodyFee, billing)
    fun getCustodyFee(): HashMap<String, HashMap<String, Billing>> = getBilling(custodyFee)
    fun getCustodyFee(domain: String, asset: String): Billing =
        getBilling(custodyFee, domain, asset, Billing.BillingTypeEnum.CUSTODY)

    private fun addAccountCreationFee(billing: Billing) = addBilling(accountCreationFee, billing)
    fun getAccountCreationFee(): HashMap<String, HashMap<String, Billing>> = getBilling(accountCreationFee)
    fun getAccountCreationFee(domain: String, asset: String): Billing =
        getBilling(accountCreationFee, domain, asset, Billing.BillingTypeEnum.ACCOUNT_CREATION)

    private fun addExchangeFee(billing: Billing) = addBilling(exchangeFee, billing)
    fun getExchangeFee(): HashMap<String, HashMap<String, Billing>> = getBilling(exchangeFee)
    fun getExchangeFee(domain: String, asset: String): Billing =
        getBilling(exchangeFee, domain, asset, Billing.BillingTypeEnum.EXCHANGE)

    private fun addWithdrawalFee(billing: Billing) = addBilling(withdrawalFee, billing)
    fun getWithdrawalFee(): HashMap<String, HashMap<String, Billing>> = getBilling(withdrawalFee)
    fun getWithdrawalFee(domain: String, asset: String): Billing =
        getBilling(withdrawalFee, domain, asset, Billing.BillingTypeEnum.WITHDRAWAL)

    @Synchronized
    private fun addBilling(billingMap: HashMap<String, HashMap<String, Billing>>, billing: Billing) {
        val domain = billing.domainName
        if (!billingMap.contains(domain)) {
            billingMap[domain] = HashMap()
        }
        billingMap[domain]!![billing.asset] = billing
    }

    @Synchronized
    private fun getBilling(billingMap: HashMap<String, HashMap<String, Billing>>): HashMap<String, HashMap<String, Billing>> {
        val copy = HashMap<String, HashMap<String, Billing>>()
        copy.putAll(billingMap)
        return copy
    }

    @Synchronized
    private fun getBilling(
        billingMap: HashMap<String, HashMap<String, Billing>>,
        domain: String,
        assetId: String,
        title: Billing.BillingTypeEnum
    ): Billing {
        if (billingMap.contains(domain)) {
            if (billingMap[domain]!!.contains(assetId)) {
                return billingMap[domain]!![assetId]!!
            }
        }
        throw RuntimeException("No ${title.name} billing found for: $domain, $assetId")
    }

    @PostConstruct
    fun init() {
        dbService.billingRepo.findAll().forEach {
            addFeeByType(it)
        }
    }

    companion object : KLogging()
}
