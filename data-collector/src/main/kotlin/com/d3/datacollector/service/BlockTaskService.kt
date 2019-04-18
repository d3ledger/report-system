package com.d3.datacollector.service

import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.*
import com.d3.datacollector.repository.BlockRepository
import com.d3.datacollector.repository.CreateAccountRepo
import com.d3.datacollector.repository.TransactionRepo
import com.d3.datacollector.repository.TransferAssetRepo
import com.d3.datacollector.utils.irohaBinaryKeyfromHex
import com.google.protobuf.ProtocolStringList
import iroha.protocol.Commands
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.Utils
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import javax.transaction.Transactional

@Service
class BlockTaskService {
    private val log = KLogging().logger
    @Autowired
    lateinit var dbService: DbService
    @Autowired
    private lateinit var cache: CacheRepository
    @Value("\${iroha.toriiAddress}")
    lateinit var toriiAddress: String
    @Value("\${iroha.user.privateKeyHex}")
    private lateinit var privateKey: String
    @Value("\${iroha.user.publicKeyHex}")
    private lateinit var publicKey: String
    @Value("\${iroha.user.id}")
    private lateinit var userId: String
    @Value("\${iroha.transferBillingTemplate}")
    private lateinit var transferBillingTemplate: String
    @Value("\${iroha.custodyBillingTemplate}")
    private lateinit var custodyBillingTemplate: String
    @Value("\${iroha.accountCreationBillingTemplate}")
    private lateinit var accountCreationBillingTemplate: String
    @Value("\${iroha.exchangeBillingTemplate}")
    private lateinit var exchangeBillingTemplate: String
    @Value("\${iroha.withdrawalBillingTemplate}")
    private lateinit var withdrawalBillingTemplate: String
    val LAST_PROCESSED_BLOCK_ROW_ID = 0L
    val LAST_REQUEST_ROW_ID = 1L
    private var api: IrohaAPI? = null
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

    @Transactional
    fun processBlockTask(): Boolean {
        val lastBlockState = dbService.stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID).get()
        val lastRequest = dbService.stateRepo.findById(LAST_REQUEST_ROW_ID).get()
        val newBlockNumber = lastBlockState.value.toLong() + 1
        val newRequestNumber = lastRequest.value.toLong() + 1
        val response = irohaBlockQuery(newBlockNumber, newRequestNumber)

        if (response.hasBlockResponse()) {
            log.debug("Successful Iroha block query: $response")

            if (response.blockResponse.block.hasBlockV1()) {
                val blockV1 = response.blockResponse.block.blockV1
                val rejectedTrxs = blockV1.payload.rejectedTransactionsHashesList
                val dbBlock = blockRepo.save(Block(newBlockNumber, blockV1.payload.createdTime))
                blockV1.payload.transactionsList.forEach { tx ->
                    val reducedPayload = tx.payload.reducedPayload
                    var dbTransaction = transactionRepo.save(Transaction(
                        block = dbBlock,
                        creatorId = reducedPayload.creatorAccountId,
                        quorum = reducedPayload.quorum,
                        rejected = !checkTrxAccepted(tx, rejectedTrxs)
                    ))
                    reducedPayload
                        .commandsList
                        .stream()
                        .forEach {
                            if (it.hasSetAccountDetail()) {
                                processBillingAccountDetail(it.setAccountDetail)
                            } else if (it.hasTransferAsset()) {
                                val assetTransfer = it.transferAsset
                                transferRepo.save(
                                    TransferAsset(
                                        assetTransfer.srcAccountId,
                                        assetTransfer.destAccountId,
                                        assetTransfer.assetId,
                                        assetTransfer.description,
                                        BigDecimal(assetTransfer.amount),
                                        dbTransaction
                                    )
                                )
                            } else if(it.hasCreateAccount()) {
                                val ca = it.createAccount
                                createAccountRepo.save(CreateAccount(
                                    ca.accountName,
                                    ca.domainId,
                                    ca.publicKey,
                                    dbTransaction
                                ))
                            }
                        }
                }
            } else {
                log.error("Block response of unsupported version: $response")
            }
            dbService.updateStateInDb(lastBlockState, lastRequest)
            return true
        } else if (response.hasErrorResponse()) {
            if (response.errorResponse.errorCode == 3) {
                log.debug("Highest block riched. Finishing blocks downloading job execution")
            } else {
                val error = response.errorResponse
                log.error("Blocks querying job error: errorCode: ${error.errorCode}, message: ${error.message}")
            }
            return true
        } else {
            log.error("No block or error response caught from Iroha: $response")
            return false
        }
    }

    private fun processBillingAccountDetail(ad: Commands.SetAccountDetail) {
        if (filterBillingAccounts(ad)) {
            val billing = Billing(
                null,
                ad.accountId,
                defineBillingType(ad.accountId),
                ad.key.replace("_", "#"),
                BigDecimal(ad.value)
            )
            performUpdates(billing)
        }
    }

    private fun checkTrxAccepted(
        tx: TransactionOuterClass.Transaction?,
        rejectedTrxs: ProtocolStringList
    ): Boolean {
        val trxHash = Utils.toHex(Utils.hash(tx))
        var accepted = true
        rejectedTrxs.forEach {
            if (it.contentEquals(trxHash)) {
                accepted = false
            }
        }
        return accepted
    }

    private fun performUpdates(billing: Billing) {
        val updated = dbService.updateBillingInDb(billing)
        cache.addFeeByType(updated)
        rabbitService.sendBillingUpdate(
            BillingMqDto(
                updated.accountId,
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
        return accountId.contains(
            transferBillingTemplate
        ) || accountId.contains(
            custodyBillingTemplate
        ) || accountId.contains(
            accountCreationBillingTemplate
        ) || accountId.contains(
            exchangeBillingTemplate
        ) || accountId.contains(
            withdrawalBillingTemplate
        )
    }

    private fun defineBillingType(accountId: String): Billing.BillingTypeEnum = when {
        accountId.contains(transferBillingTemplate) -> Billing.BillingTypeEnum.TRANSFER
        accountId.contains(custodyBillingTemplate) -> Billing.BillingTypeEnum.CUSTODY
        accountId.contains(accountCreationBillingTemplate) -> Billing.BillingTypeEnum.ACCOUNT_CREATION
        accountId.contains(exchangeBillingTemplate) -> Billing.BillingTypeEnum.EXCHANGE
        accountId.contains(withdrawalBillingTemplate) -> Billing.BillingTypeEnum.WITHDRAWAL
        else -> Billing.BillingTypeEnum.NOT_FOUND
    }

    fun irohaBlockQuery(
        newRequestNumber: Long,
        newBlock: Long
    ): QryResponses.QueryResponse {
        val q = Query.builder(userId, newRequestNumber + 1)
            .getBlock(newBlock)
            .buildSigned(
                Ed25519Sha3.keyPairFromBytes(
                    irohaBinaryKeyfromHex(privateKey),
                    irohaBinaryKeyfromHex(publicKey)
                )
            )
        if (api == null) {
            api = IrohaAPI(URI(toriiAddress))
        }
        val response = api!!.query(q)
        return response
    }
}
