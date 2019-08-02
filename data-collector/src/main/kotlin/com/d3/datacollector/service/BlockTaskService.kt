/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.service

import com.d3.commons.config.RMQConfig
import com.d3.commons.sidechain.iroha.ReliableIrohaChainListener
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.config.AppConfig.Companion.queueName
import com.d3.datacollector.model.*
import com.d3.datacollector.repository.*
import com.d3.datacollector.utils.getDomainFromAccountId
import com.github.kittinunf.result.map
import com.google.protobuf.ProtocolStringList
import io.reactivex.schedulers.Schedulers
import iroha.protocol.BlockOuterClass
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.ErrorResponseException
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.io.Closeable
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service
class BlockTaskService : Closeable {

    @Autowired
    lateinit var dbService: DbService
    @Autowired
    private lateinit var cache: CacheRepository
    @Value("\${iroha.templates.transferBilling}")
    private lateinit var transferBillingTemplate: String
    @Value("\${iroha.templates.custodyBilling}")
    private lateinit var custodyBillingTemplate: String
    @Value("\${iroha.templates.accountCreationBilling}")
    private lateinit var accountCreationBillingTemplate: String
    @Value("\${iroha.templates.exchangeBilling}")
    private lateinit var exchangeBillingTemplate: String
    @Value("\${iroha.templates.withdrawalBilling}")
    private lateinit var withdrawalBillingTemplate: String
    @Value("\${iroha.latticePlaceholder}")
    private lateinit var latticePlaceholder: String
    @Autowired
    lateinit var rabbitService: RabbitMqService
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var transactionRepo: TransactionRepo
    @Autowired
    lateinit var transferRepo: TransferAssetRepo
    @Autowired
    lateinit var createAccountRepo: CreateAccountRepo
    @Autowired
    lateinit var createAssetRepo: CreateAssetRepo
    @Autowired
    lateinit var accountDetailRepo: SetAccountDetailRepo
    @Autowired
    lateinit var accountQuorumRepo: SetAccountQuorumRepo
    @Autowired
    lateinit var addSignatoryRepo: AddSignatoryRepository
    @Autowired
    lateinit var txBatchRepo: TransactionBatchRepo
    @Lazy
    @Autowired
    lateinit var rmqConfig: RMQConfig
    @Autowired
    lateinit var irohaBlockService: IrohaBlockService

    private val irohaChainListener by lazy {
        ReliableIrohaChainListener(
            rmqConfig,
            queueName
        )
    }

    private val isStarted = AtomicBoolean()
    private val scheduler = Schedulers.from(
        createPrettySingleThreadPool(
            "data-collector",
            "block-task-service"
        )
    )

    fun runService() {
        if (!isStarted.compareAndSet(false, true)) {
            return
        }
        logger.info { "Starting dc block processor" }
        processMissedBlocks()
        irohaChainListener.getBlockObservable().map { observable ->
            observable.observeOn(scheduler).subscribe { (block, _) ->
                parseBlock(block)
            }
        }
        irohaChainListener.listen()
    }

    private fun processMissedBlocks() {
        var lastBlockProcessed = dbService.getLastBlockProcessed() + 1
        do {
            try {
                logger.info { "Requesting $lastBlockProcessed block from Iroha" }
                parseBlock(irohaBlockService.irohaBlockQuery(lastBlockProcessed++).block)
            } catch (e: ErrorResponseException) {
                logger.warn("Got exception, finishing Iroha blocks querying. ${e.message}", e)
                return
            }
        } while (true)
    }

    private fun parseBlock(block: BlockOuterClass.Block) {
        logger.debug { "Got new block: ${block.allFields}" }
        if (block.hasBlockV1()) {
            val blockV1 = block.blockV1
            val height = blockV1.payload.height
            logger.info { "Incoming block height is $height" }
            val lastBlockSeen = dbService.getLastBlockSeen()
            if (lastBlockSeen >= height) {
                logger.error { "Block $height has already been seen" }
                return
            }
            dbService.markBlockSeen(height)
            val dbBlock = blockRepo.save(Block(height, blockV1.payload.createdTime))
            val rejectedTrxs = blockV1.payload.rejectedTransactionsHashesList
            val transactionBatches = constructBatches(blockV1.payload.transactionsList)

            transactionBatches.forEach { transactionBatch ->
                var complexBatch: TransactionBatchEntity? = null
                if (transactionBatch.isBatch()) {
                    complexBatch = TransactionBatchEntity(
                        batchType = if (transactionBatch.hasTransferTo(exchangeBillingTemplate))
                            TransactionBatchEntity.BatchType.EXCHANGE
                        else
                            TransactionBatchEntity.BatchType.UNDEFINED
                    )
                    complexBatch = txBatchRepo.save(complexBatch)
                }

                transactionBatch.forEach { transaction ->
                    val reducedPayload = transaction.payload.reducedPayload
                    val commitedTransaction = transactionRepo.save(
                        Transaction(
                            block = dbBlock,
                            creatorId = reducedPayload.creatorAccountId,
                            quorum = reducedPayload.quorum,
                            rejected = isTransactionRejected(transaction, rejectedTrxs),
                            batch = complexBatch
                        )
                    )
                    reducedPayload
                        .commandsList
                        .forEach { command ->
                            logger.debug("Command received: $command")
                            when {
                                command.hasSetAccountDetail() -> {
                                    processBillingAccountDetail(command.setAccountDetail)
                                    val setAccountDetail = command.setAccountDetail
                                    accountDetailRepo.save(
                                        SetAccountDetail(
                                            setAccountDetail.accountId,
                                            setAccountDetail.key,
                                            setAccountDetail.value,
                                            commitedTransaction
                                        )
                                    )
                                }
                                command.hasTransferAsset() -> {
                                    val transferAsset = command.transferAsset
                                    transferRepo.save(
                                        TransferAsset(
                                            transferAsset.srcAccountId,
                                            transferAsset.destAccountId,
                                            transferAsset.assetId,
                                            transferAsset.description,
                                            BigDecimal(transferAsset.amount),
                                            commitedTransaction
                                        )
                                    )
                                }
                                command.hasCreateAccount() -> {
                                    val createAccount = command.createAccount
                                    createAccountRepo.save(
                                        CreateAccount(
                                            createAccount.accountName,
                                            createAccount.domainId,
                                            createAccount.publicKey,
                                            commitedTransaction
                                        )
                                    )
                                }
                                command.hasCreateAsset() -> {
                                    val createAsset = command.createAsset
                                    createAssetRepo.save(
                                        CreateAsset(
                                            createAsset.assetName,
                                            createAsset.domainId,
                                            createAsset.precision,
                                            commitedTransaction
                                        )
                                    )
                                }
                                command.hasSetAccountQuorum() -> {
                                    val setAccountQuorum = command.setAccountQuorum
                                    accountQuorumRepo.save(
                                        SetAccountQuorum(
                                            setAccountQuorum.accountId,
                                            setAccountQuorum.quorum,
                                            commitedTransaction
                                        )
                                    )
                                }
                                command.hasAddSignatory() -> {
                                    val addSignatory = command.addSignatory
                                    addSignatoryRepo.save(
                                        AddSignatory(
                                            addSignatory.accountId,
                                            addSignatory.publicKey,
                                            commitedTransaction
                                        )
                                    )
                                }
                            }
                        }
                }
            }
            dbService.markBlockProcessed(height)
        } else {
            logger.error("Block response of unsupported version: ${block.blockVersionCase}")
        }
    }

    private fun processBillingAccountDetail(ad: Commands.SetAccountDetail) {
        if (filterBillingAccounts(ad)) {
            if (ad.value.length > 7 || !ad.value.contains('.') || ad.value.indexOf('.') > 2) {
                return
            }
            val billing = Billing(
                null,
                getDomainFromAccountId(ad.accountId),
                defineBillingType(ad.accountId),
                ad.key.replace(latticePlaceholder, "#"),
                BigDecimal(ad.value)
            )
            performUpdates(billing)
        }
    }

    private fun isTransactionRejected(
        tx: TransactionOuterClass.Transaction,
        rejectedTrxs: ProtocolStringList
    ): Boolean {
        val hash = Utils.toHex(Utils.hash(tx))
        return rejectedTrxs.any { rejected ->
            rejected!!.equals(hash, true)
        }
    }

    private fun performUpdates(billing: Billing) {
        val updated = dbService.updateBillingInDb(billing)
        cache.addFeeByType(updated)
        rabbitService.sendBillingUpdate(
            BillingMqDto(
                updated.domainName,
                updated.billingType,
                updated.asset,
                updated.feeFraction,
                updated = updated.updated,
                created = updated.created
            )
        )
    }

    private fun filterBillingAccounts(it: Commands.SetAccountDetail): Boolean {
        val accountId = it.accountId
        return accountId.startsWith(
            transferBillingTemplate
        ) || accountId.startsWith(
            custodyBillingTemplate
        ) || accountId.startsWith(
            accountCreationBillingTemplate
        ) || accountId.startsWith(
            exchangeBillingTemplate
        ) || accountId.startsWith(
            withdrawalBillingTemplate
        )
    }

    private fun defineBillingType(accountId: String): Billing.BillingTypeEnum = when {
        accountId.startsWith(transferBillingTemplate) -> Billing.BillingTypeEnum.TRANSFER
        accountId.startsWith(custodyBillingTemplate) -> Billing.BillingTypeEnum.CUSTODY
        accountId.startsWith(accountCreationBillingTemplate) -> Billing.BillingTypeEnum.ACCOUNT_CREATION
        accountId.startsWith(exchangeBillingTemplate) -> Billing.BillingTypeEnum.EXCHANGE
        accountId.startsWith(withdrawalBillingTemplate) -> Billing.BillingTypeEnum.WITHDRAWAL
        else -> Billing.BillingTypeEnum.NOT_FOUND
    }

    /**
     * Converts Iroha transactions to Data Collector batches
     *
     * @param transactions transaction list to be converted
     * @return [List] of [TransactionBatch] of input
     */
    private fun constructBatches(transactions: List<TransactionOuterClass.Transaction>): List<TransactionBatch> {
        val transactionBatches = ArrayList<TransactionBatch>()
        var i = 0
        while (i < transactions.size) {
            val transactionListForBatch = ArrayList<TransactionOuterClass.Transaction>()
            val hashesCount = transactions[i]
                .payload
                .batch
                .reducedHashesCount
            val toInclude = if (hashesCount == 0) 1 else hashesCount

            for (j in 0 until toInclude) {
                transactionListForBatch.add(transactions[i + j])
            }
            i += toInclude
            transactionBatches.add(TransactionBatch(transactionListForBatch))
        }
        return transactionBatches
    }

    override fun close() {
        irohaChainListener.close()
    }

    companion object : KLogging()
}
