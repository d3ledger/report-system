package com.d3.report.controllers

import com.d3.report.context.AccountCustodyContext
import com.d3.report.context.AssetCustodyContext
import com.d3.report.model.*
import com.d3.report.repository.BillingRepository
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.repository.TransferAssetRepo
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.math.BigDecimal
import java.util.stream.Collectors
import javax.transaction.Transactional
import javax.validation.constraints.NotNull
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
@Controller
@RequestMapping("/report/billing/custody")
class CustodyController {

    companion object {
        val log = KLogging().logger
    }

    @Autowired
    private lateinit var transaferRepo: TransferAssetRepo
    @Autowired
    private lateinit var accountRepo: CreateAccountRepo
    @Autowired
    private lateinit var billingRepo: BillingRepository
    @Value("\${iroha.templates.custodyBilling}")
    private lateinit var custodyAccountTemplate: String
    @Value("\${billing.custody.period}")
    private var custodyPeriod: Long = 86400000

    /**
     * Add from parameter and svaing of daily snapshots.
     * To calculate fees for a period on a finished dayly basis. Not to recalculate all values for every request.
     */
    @GetMapping("/agent")
    @Transactional
    fun reportBillingTransferAsset(
        @NotNull @RequestParam domain: String,
        //    @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<CustodyReport> {
        val billingStore = HashMap<String, Billing>()
        return try {
            val accountsPage =
                accountRepo.getDomainAccounts(domain, PageRequest.of(pageNum - 1, pageSize))

            /*
                 Collection with custody Fee
            */
            val custodyFees = HashMap<String, AccountCustody>()
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

                            if (assetCustodyContextForAccount.lastTransferTimestamp != null) {
                                addFeePortion(
                                    assetCustodyContextForAccount,
                                    transfer.transaction.block!!.blockCreationTime!!,
                                    billing.feeFraction
                                )
                            }
                            assetCustodyContextForAccount.lastTransferTimestamp =
                                transfer.transaction.block!!.blockCreationTime
                            if (transfer.destAccountId!!.contentEquals(getAccountId(account))) {
                                assetCustodyContextForAccount.lastAssetSum.add(transfer.amount)
                            } else if (transfer.srcAccountId!!.contentEquals(getAccountId(account))) {
                                assetCustodyContextForAccount.lastAssetSum.subtract(transfer.amount)
                            }
                        }
                    calculatedTransferPages += 1
                } while (++calculatedTransferPages - transfersPage.totalPages < 0)

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

            ResponseEntity.ok(
                CustodyReport(
                    accounts = custodyFees.values.stream().collect(Collectors.toList()),
                    total = accountsPage.totalElements,
                    pages = accountsPage.totalPages
                )
            )
        } catch (e: Exception) {
            log.error("Error making Custody report: ", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                CustodyReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    /**
     * @param assetCustodyContextForAccount - context of fee calculation for asset in account
     * @param blockCreationTime - time of new transfer transaction block commit
     * @param feeFraction - Fraction of money that should be sent as fee.
     */
    private fun addFeePortion(
        assetCustodyContextForAccount: AssetCustodyContext,
        blockCreationTime: Long,
        feeFraction: BigDecimal
    ) {
        val previous =
            BigDecimal(assetCustodyContextForAccount.lastTransferTimestamp.toString())
        val new =
            BigDecimal(blockCreationTime.toString())
        val length = new.minus(previous)
        val custodyMultiplier = length.divide(BigDecimal(custodyPeriod))
        val periodFee = feeFraction.multiply(custodyMultiplier)
        assetCustodyContextForAccount
            .commulativeFeeAmount =
            assetCustodyContextForAccount.commulativeFeeAmount.add(periodFee)
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

    private fun getAccountId(account: CreateAccount) =
        "${account.accountName}@${account.domainId}"
}
