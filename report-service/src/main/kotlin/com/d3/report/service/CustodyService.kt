/*
 * Copyright Soramitsu Co., Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.service

import com.d3.report.model.*
import com.d3.report.repository.AccountAssetCustodyContextRepo
import com.d3.report.repository.BillingRepository
import com.d3.report.repository.TransferAssetRepo
import com.d3.report.utils.getAccountId
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CustodyService(
    val transferRepo: TransferAssetRepo,
    val billingRepo: BillingRepository,
    val custodyContextRepo: AccountAssetCustodyContextRepo
) {

    companion object : KLogging()

    @Value("\${billing.custody.period}")
    private var custodyPeriod: Long = 86400000

    @Value("\${iroha.templates.custodyBilling}")
    private lateinit var custodyAccountTemplate: String

    /**
     * Calculates AUC(Asset Under Custody)
     * @param assetCustodyContextForAccount - context of fee calculation for asset in account
     * @param totalPeriod - total report period
     * @param currentPeriod - current period
     * @param periodAssetSum - sum assets in current period
     */
    fun addAUC(
        assetCustodyContextForAccount: AssetCustodyContext,
        totalPeriod: Long,
        currentPeriod: Long,
        periodAssetSum: BigDecimal
    ) {
        val relativePeriod = currentPeriod.toBigDecimal().divide(totalPeriod.toBigDecimal())
        val relativeSum = periodAssetSum.multiply(relativePeriod)
        assetCustodyContextForAccount.cumulativeSum += relativeSum
    }

    /**
     * @param assetCustodyContextForAccount - context of fee calculation for asset in account
     * @param currentTo - time of new transfer transaction block commit
     * @param feeFraction - Fraction of money that should be sent as fee.
     */
    fun addFeePortion(
        assetCustodyContextForAccount: AssetCustodyContext,
        currentTo: Long,   // When current amount of asset in account will be changed
        feeFraction: BigDecimal
    ) {
        if (feeFraction.compareTo(BigDecimal.ZERO) < 1) {
            throw InvalidValue("Fee fraction is zero or negative: $feeFraction")
        }

        if (feeFraction.compareTo(BigDecimal.ONE) == 1) {
            throw InvalidValue("Fee fraction should be between 0 and 1, but now we have: $feeFraction")
        }

        val previous =
            BigDecimal(assetCustodyContextForAccount.lastControlTimestamp.toString())
        val new =
            BigDecimal(currentTo.toString())
        val length = new.minus(previous)
        val custodyMultiplier = length.divide(BigDecimal(custodyPeriod), 8, RoundingMode.HALF_UP)
        val periodFeeMultiplier = feeFraction.multiply(custodyMultiplier)
        val fee = periodFeeMultiplier.multiply(assetCustodyContextForAccount.lastAssetSum)
        assetCustodyContextForAccount
            .cumulativeFeeAmount =
            assetCustodyContextForAccount.cumulativeFeeAmount.add(fee)
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
            processAccount(account, from, to, billingStore, custodyFees)
        }
    }

    fun processAccount(
        account: CreateAccount,
        from: Long,
        to: Long,
        billingStore: HashMap<String, Billing>,
        custodyFees: HashMap<String, AccountCustody>
    ) {
        val custodyContext = HashMap<String, AccountCustodyContext>()

        val accountCustodyContext = custodyContext.computeIfAbsent(account.accountName!!) {
            AccountCustodyContext()
        }

        var calculatedTransferPages = 0
        do {
            val transfersPage = getTransferPage(account, to, calculatedTransferPages)
            processTransferPage(
                transfersPage,
                accountCustodyContext,
                account,
                billingStore,
                from,
                to
            )
            calculatedTransferPages += 1
        } while (calculatedTransferPages - transfersPage.totalPages < 0)

        val assetCustodies = HashMap<String, Fee>()
        accountCustodyContext.assetsContexts.forEach {
            /**
             * Calculation algorithm step 3.
             * Calculate Commissions between last transfer for the selected period and 'to' date
             */
            if (billingStore[it.key] != null) {
                val assetCustodyContextForAccount = it.value
                addAUC(
                    assetCustodyContextForAccount,
                    to - from,
                    to - assetCustodyContextForAccount.lastControlTimestamp,
                    assetCustodyContextForAccount.lastAssetSum
                )
                addFeePortion(assetCustodyContextForAccount, to, billingStore[it.key]!!.feeFraction)
                assetCustodies[it.key] =
                    Fee(
                        assetCustodyContextForAccount.cumulativeFeeAmount,
                        assetCustodyContextForAccount.cumulativeSum
                    )
            }
        }
        custodyFees[getAccountId(account)] = AccountCustody(getAccountId(account), assetCustodies)
    }

    private fun processTransferPage(
        transfersPage: Page<TransferAsset>,
        accountCustodyContext: AccountCustodyContext,
        account: CreateAccount,
        billingStore: HashMap<String, Billing>,
        from: Long,
        to: Long
    ) {
        transfersPage
            .get()
            .forEach { transfer ->
                try {
                    processTransfer(
                        accountCustodyContext,
                        transfer,
                        account,
                        from,
                        to,
                        billingStore,
                        transfer.assetId!!
                    )
                } catch (e: BillingNotFoundException) {
                    logger.error("Skipping transfer because of exception", e)
                }
            }
    }

    private fun processTransfer(
        accountCustodyContext: AccountCustodyContext,
        transfer: TransferAsset,
        account: CreateAccount,
        from: Long,
        to: Long,
        billingStore: HashMap<String, Billing>,
        assetId: String
    ) {

        val assetCustodyContextForAccount =
            getAssetCustodyContext(
                accountCustodyContext,
                transfer,
                account,
                from
            )

        val billing = getBillingProperties(billingStore, transfer, account, assetId)

        /**
         * Fee calculation algorithm step 2
         * Calculate commissions between control points and transfers inside report period
         */
        if (assetCustodyContextForAccount.lastControlTimestamp > from) {
            val blockCreationTime = transfer.transaction?.block!!.blockCreationTime
            addAUC(
                assetCustodyContextForAccount,
                to - from,
                blockCreationTime - assetCustodyContextForAccount.lastControlTimestamp,
                assetCustodyContextForAccount.lastAssetSum + transfer.amount!!
            )
            addFeePortion(
                assetCustodyContextForAccount,
                blockCreationTime,
                billing.feeFraction
            )
        }

        updateAssetCustodyContextForAccount(
            assetCustodyContextForAccount,
            transfer,
            account,
            billing.feeFraction,
            from,
            to
        )

        manageContextSnapshotsInDb(
            account,
            assetId,
            from,
            assetCustodyContextForAccount
        )
    }

    private fun getBillingProperties(
        billingStore: HashMap<String, Billing>,
        transfer: TransferAsset,
        account: CreateAccount,
        assetId: String
    ): Billing {
        return billingStore.computeIfAbsent(
            transfer.assetId!!
        )
        {
            val billing = billingRepo.selectByAccountIdBillingTypeAndAsset(
                "$custodyAccountTemplate${account.domainId}",
                assetId,
                Billing.BillingTypeEnum.CUSTODY
            )
            if (billing.isPresent) {
                billing.get()
            } else {
                val errorMessage =
                    "Billing ${Billing.BillingTypeEnum.CUSTODY} not found for: domain=${account.domainId}, asset: $assetId"
                throw BillingNotFoundException(errorMessage)
            }
        }
    }

    private fun updateAssetCustodyContextForAccount(
        assetCustodyContextForAccount: AssetCustodyContext,
        transfer: TransferAsset,
        account: CreateAccount,
        feeFraction: BigDecimal,
        from: Long,
        to: Long
    ) {
        val blockCreationTime = transfer.transaction?.block!!.blockCreationTime
        if (assetCustodyContextForAccount.lastControlTimestamp < blockCreationTime
        ) {

            /**
             * Fee calculation Algorithm step 1
             * Calculate fee from 'from' date to first transfer
             */
            addAUC(
                assetCustodyContextForAccount,
                to - from,
                blockCreationTime - from,
                assetCustodyContextForAccount.lastAssetSum
            )
            addFeePortion(
                assetCustodyContextForAccount,
                blockCreationTime,
                feeFraction
            )
            assetCustodyContextForAccount.lastControlTimestamp =
                blockCreationTime
        }

        if (transfer.destAccountId!!.contentEquals(getAccountId(account))) {
            assetCustodyContextForAccount.lastAssetSum =
                assetCustodyContextForAccount.lastAssetSum.add(transfer.amount)
        } else if (transfer.srcAccountId!!.contentEquals(getAccountId(account))) {
            assetCustodyContextForAccount.lastAssetSum =
                assetCustodyContextForAccount.lastAssetSum.subtract(transfer.amount)
        }
    }

    private fun manageContextSnapshotsInDb(
        account: CreateAccount,
        assetId: String,
        from: Long,
        assetCustodyContextForAccount: AssetCustodyContext
    ) {
        val option = custodyContextRepo
            .selectByTimeAndAccountAndAssetId(getAccountId(account), assetId, from)
        if (option.isPresent) { // Check that there are any snapshot suited for this timestamp
            val context = option.get()
            /* Check if snapshot from database is too old we should add new snapshot*/
            if (isSnapshotFromDbTooOld(context, assetCustodyContextForAccount)) {
                saveCustodyContext(account, assetId, assetCustodyContextForAccount)
            }
        } else { // If no snapshots found in database we should add this one
            saveCustodyContext(account, assetId, assetCustodyContextForAccount)
        }
    }

    private fun isSnapshotFromDbTooOld(
        context: AccountAssetCustodyContext,
        assetCustodyContextForAccount: AssetCustodyContext
    ) =
        context.lastTransferTimestamp.minus(assetCustodyContextForAccount.lastControlTimestamp) > custodyPeriod * 14

    private fun saveCustodyContext(
        account: CreateAccount,
        assetId: String,
        assetCustodyContextForAccount: AssetCustodyContext
    ) {
        custodyContextRepo.save(
            AccountAssetCustodyContext(
                accountId = getAccountId(account),
                assetId = assetId,
                commulativeFeeAmount = assetCustodyContextForAccount.cumulativeFeeAmount,
                lastTransferTimestamp = assetCustodyContextForAccount.lastControlTimestamp,
                lastAssetSum = assetCustodyContextForAccount.lastAssetSum
            )
        )
    }

    private fun getAssetCustodyContext(
        accountCustodyContext: AccountCustodyContext,
        transfer: TransferAsset,
        account: CreateAccount,
        from: Long
    ): AssetCustodyContext {
        /* if (!accountCustodyContext.assetsContexts.containsKey(transfer.assetId!!)) {
             val option = custodyContextRepo
                 .selectByTimeAndAccountAndAssetId(getAccountId(account), transfer.assetId, from)
             if (option.isPresent) {
                 val assetContext = option.get()
                 accountCustodyContext.assetsContexts.put(
                     transfer.assetId, AssetCustodyContext(
                         cumulativeFeeAmount = assetContext.cumulativeFeeAmount,
                         lastControlTimestamp = assetContext.lastControlTimestamp,
                         lastAssetSum = assetContext.lastAssetSum
                     )
                 )
             }
         }*/
        /*
          Sum of custody fees for asset from every period
        */
        val assetCustodyContextForAccount =
            accountCustodyContext.assetsContexts.computeIfAbsent(transfer.assetId!!) {
                AssetCustodyContext(
                    /**
                     * Set starting control point to 'from' date for reporting
                     * period because we are not interested to calculate fee for previous points.
                     */
                    lastControlTimestamp = from
                )
            }
        return assetCustodyContextForAccount
    }

    private fun getTransferPage(
        account: CreateAccount,
        to: Long,
        pageNum: Int
    ): Page<TransferAsset> {
        return transferRepo.getAllTransfersForAccountInAndOutTillTo(
            getAccountId(account),
            to,
            PageRequest.of(pageNum, 200)
        )
    }
}
