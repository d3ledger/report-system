package com.d3.datacollector.service

import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.BillingMqDto
import com.d3.datacollector.utils.irohaBinaryKeyfromHex
import iroha.protocol.Commands
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
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

    @Transactional
    fun processBlockTask(): Boolean {

        val lastBlockState = dbService.stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID).get()
        val lastRequest = dbService.stateRepo.findById(LAST_REQUEST_ROW_ID).get()

        val response = irohaBlockQuery(lastRequest.value.toLong(), lastBlockState.value.toLong())


        if (response.hasBlockResponse()) {
            log.debug("Successful Iroha block query: $response")

            if (response.blockResponse.block.hasBlockV1()) {
                val blockV1 = response.blockResponse.block.blockV1
                blockV1.payload.transactionsList.forEach { transaction ->
                    transaction
                        .payload
                        .reducedPayload
                        .commandsList
                        .stream()
                        .filter { it.hasSetAccountDetail() }
                        .map { it.setAccountDetail }
                        .filter { filterBillingAccounts(it) }
                        .forEach {
                            val billing = Billing(
                                null,
                                it.accountId,
                                defineBillingType(it.accountId),
                                it.key,
                                BigDecimal(it.value)
                            )
                            performUpdates(billing)
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

    private fun performUpdates(billing: Billing) {
        dbService.updateBillingInDb(billing)
        cache.funAddFeebyType(billing)
        rabbitService.sendBillingUpdate(
            BillingMqDto(
                billing.accountId,
                billing.billingType,
                billing.asset,
                billing.feeFraction,
                billing.updated
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
        lastRequest: Long,
        lastBlock: Long
    ): QryResponses.QueryResponse {


        val q = Query.builder(userId, lastRequest + 1)
            .getBlock(lastBlock + 1)
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
