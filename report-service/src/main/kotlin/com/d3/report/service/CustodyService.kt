/*
 *
 *  Copyright D3 Ledger, Inc. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 * /
 */

package com.d3.report.service

import com.d3.report.model.AccountCustodyContext
import com.d3.report.model.AssetCustodyContext
import com.d3.report.model.*
import com.d3.report.repository.BillingRepository
import com.d3.report.repository.TransferAssetRepo
import com.d3.report.utils.getAccountId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CustodyService {
    @Autowired
    private lateinit var transaferRepo: TransferAssetRepo
    @Autowired
    private lateinit var billingRepo: BillingRepository
    @Value("\${billing.custody.period}")
    private var custodyPeriod: Long = 86400000

    @Value("\${iroha.templates.custodyBilling}")
    private lateinit var custodyAccountTemplate: String

    /**
     * @param assetCustodyContextForAccount - context of fee calculation for asset in account
     * @param blockCreationTime - time of new transfer transaction block commit
     * @param feeFraction - Fraction of money that should be sent as fee.
     */
    fun addFeePortion(
        assetCustodyContextForAccount: AssetCustodyContext,
        blockCreationTime: Long,
        feeFraction: BigDecimal
    ) {
        if (feeFraction.compareTo(BigDecimal.ZERO) < 1) {
            throw InvalidValue("Fee fraction is zero or negative: $feeFraction")
        }

        if (feeFraction.compareTo(BigDecimal.ONE) == 1) {
            throw InvalidValue("Fee fraction should be between 0 and 1, but now we have: $feeFraction")
        }

        val previous =
            BigDecimal(assetCustodyContextForAccount.lastTransferTimestamp.toString())
        val new =
            BigDecimal(blockCreationTime.toString())
        val length = new.minus(previous)
        val custodyMultiplier = length.divide(BigDecimal(custodyPeriod), 8, RoundingMode.HALF_UP)
        val periodFeeMultiplier = feeFraction.multiply(custodyMultiplier)
        val fee = periodFeeMultiplier.multiply(assetCustodyContextForAccount.lastAssetSum)
        assetCustodyContextForAccount
            .commulativeFeeAmount =
            assetCustodyContextForAccount.commulativeFeeAmount.add(fee)
    }

    fun processAccounts(
        accountsPage: Page<CreateAccount>,
        pageNum: Int,
        billingStore: HashMap<String, Billing>,
        from: Long,
        to: Long,
        custodyFees: HashMap<String, AccountCustody>
    ) {
        accountsPage.forEach { account ->
            /*
                Calculation context for asset of account
                 */
            val custodyContext = HashMap<String, AccountCustodyContext>()

            val accountCustodyContext = custodyContext.computeIfAbsent(account.accountName!!) {
                AccountCustodyContext()
            }
            var calculatedTransferPages = 0
            do {
                val transfersPage = getTransferPage(account, pageNum)
                processTransferPage(
                    transfersPage,
                    accountCustodyContext,
                    account,
                    billingStore,
                    from
                )
                calculatedTransferPages += 1
            } while (calculatedTransferPages - transfersPage.totalPages < 0)

            val assetCustodies = HashMap<String, BigDecimal>()
            accountCustodyContext.assetsContexts.forEach {
                addFeePortion(it.value, to, billingStore[it.key]!!.feeFraction)
                assetCustodies.put(it.key, it.value.commulativeFeeAmount)
            }
            custodyFees.put(
                getAccountId(account),
                AccountCustody(getAccountId(account), assetCustodies)
            )
        }
    }

    private fun processTransferPage(
        transfersPage: Page<TransferAsset>,
        accountCustodyContext: AccountCustodyContext,
        account: CreateAccount,
        billingStore: HashMap<String, Billing>,
        from: Long
    ) {
        transfersPage
            .get()
            .forEach { transfer ->
                /*
                                Sum of custody fees for asset from every period
                             */
                val assetCustodyContextForAccount =
                    accountCustodyContext.assetsContexts.computeIfAbsent(transfer.assetId!!) {
                        AssetCustodyContext(
                            lastTransferTimestamp = account.transaction.block?.blockCreationTime
                        )
                    }
                /*
                                Get billing propertires
                             */
                val billing = billingStore.computeIfAbsent(
                    transfer.assetId
                )
                {
                    billingRepo.selectByAccountIdBillingTypeAndAsset(
                        "$custodyAccountTemplate@${account.domainId}",
                        transfer.assetId,
                        Billing.BillingTypeEnum.CUSTODY
                    ).get()
                }
                val lastTransferTime =
                    assetCustodyContextForAccount.lastTransferTimestamp ?: 0
                if (assetCustodyContextForAccount.lastTransferTimestamp != null && lastTransferTime > from) {
                    addFeePortion(
                        assetCustodyContextForAccount,
                        transfer.transaction.block!!.blockCreationTime!!,
                        billing.feeFraction
                    )
                }
                assetCustodyContextForAccount.lastTransferTimestamp =
                    transfer.transaction.block!!.blockCreationTime
                if (transfer.destAccountId!!.contentEquals(getAccountId(account))) {
                    assetCustodyContextForAccount.lastAssetSum =
                        assetCustodyContextForAccount.lastAssetSum.add(transfer.amount)
                } else if (transfer.srcAccountId!!.contentEquals(getAccountId(account))) {
                    assetCustodyContextForAccount.lastAssetSum =
                        assetCustodyContextForAccount.lastAssetSum.subtract(transfer.amount)
                }
            }
    }

    private fun getTransferPage(
        account: CreateAccount,
        pageNum: Int
    ): Page<TransferAsset> {
        return transaferRepo.getAllDataForAccount(
            getAccountId(account),
            PageRequest.of(pageNum - 1, 200)
        )
    }
}
