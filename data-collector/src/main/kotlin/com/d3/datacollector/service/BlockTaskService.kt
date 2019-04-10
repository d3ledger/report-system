package com.d3.datacollector.service

import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.Billing
import com.d3.datacollector.utils.irohaBinaryKeyfromHex
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
    lateinit var cache: CacheRepository

    @Value("\${iroha.toriiAddress}")
    lateinit var toriiAddress: String

    @Value("\${iroha.user.privateKeyHex}")
    lateinit var privateKey: String
    @Value("\${iroha.user.publicKeyHex}")
    lateinit var publicKey: String
    @Value("\${iroha.user.id}")
    lateinit var userId: String
    @Value("\${iroha.transferBillingTemplate}")
    lateinit var transferBillingTemplate: String

    val LAST_PROCESSED_BLOCK_ROW_ID = 0L
    val LAST_REQUEST_ROW_ID = 1L

    private var api: IrohaAPI? = null

    @Transactional
    fun processBlockTask(): Boolean {

        val lastBlockState = dbService.stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID).get()
        val lastRequest = dbService.stateRepo.findById(LAST_REQUEST_ROW_ID).get()

        val response = irohaBlockQuery(lastRequest.value.toLong(), lastBlockState.value.toLong())


        if (response.hasBlockResponse()) {
            log.info("Successful Iroha block query: $response")

            if (response.blockResponse.block.hasBlockV1()) {
                val blockV1 = response.blockResponse.block.blockV1
                blockV1.payload.transactionsList.forEach { transaction ->
                    transaction.payload.reducedPayload.commandsList.filter {
                        it.hasSetAccountDetail() && it.setAccountDetail.accountId.contains(
                            transferBillingTemplate
                        )
                    }.forEach {
                        val billing = Billing(
                            null,
                            it.setAccountDetail.accountId,
                            Billing.BillingTypeEnum.TRANSFER,
                            it.setAccountDetail.key,
                            BigDecimal(it.setAccountDetail.value)
                        )
                        dbService.updateBillingInDb(billing)
                        cache.addTransferBilling(billing)
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


    fun irohaBlockQuery(
        lastRequest: Long,
        lastBlock: Long
    ): QryResponses.QueryResponse {


        val q = Query.builder(userId, lastRequest+1)
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
