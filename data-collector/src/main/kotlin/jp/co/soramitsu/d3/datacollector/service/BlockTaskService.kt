package jp.co.soramitsu.d3.datacollector.service

import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.d3.datacollector.cache.CacheRepository
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import jp.co.soramitsu.d3.datacollector.utils.irohaBinaryKeyfromHex
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import jp.co.soramitsu.d3.datacollector.model.State
import java.math.BigDecimal


@Service
class BlockTaskService {

    val log = LoggerFactory.getLogger(BlockTaskService::class.java)

    @Autowired
    lateinit var stateRepo: StateRepository
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
    lateinit var billingAccountTemplate: String

    val LAST_PROCESSED_BLOCK_ROW_ID = 0L
    val LAST_REQUEST_ROW_ID = 1L

    private var api: IrohaAPI? = null


    fun processBlockTask() {

        val lastBlockState = stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID).get()
        val lastRequest = stateRepo.findById(LAST_REQUEST_ROW_ID).get()

        val response = irohaBlockQuery(lastRequest.value.toLong(), lastBlockState.value.toLong())


        if (response.hasBlockResponse()) {
            log.info("Successful Iroha block query: $response")

            if (response.blockResponse.block.hasBlockV1()) {
                val blockV1 = response.blockResponse.block.blockV1
                blockV1.payload.transactionsList.forEach { transaction ->
                    transaction.payload.reducedPayload.commandsList.filter {
                        it.hasSetAccountDetail() && it.setAccountDetail.accountId.contains(
                            billingAccountTemplate
                        )
                    }.forEach {
                            cache.addTransferBilling(
                                it.setAccountDetail.accountId,
                                it.setAccountDetail.key,
                                BigDecimal(it.setAccountDetail.value)
                            )
                        }
                }
            } else {
                log.error("Block response of unsupported version: $response")
            }
            updatePropertiesInDatabase(lastBlockState, lastRequest)

        } else if (response.hasErrorResponse()) {
            if (response.errorResponse.errorCode == 3) {
                log.debug("Highest block riched. Finishing blocks downloading job execution")
            } else {
                val error = response.errorResponse
                log.error("Blocks querying job error: errorCode: ${error.errorCode}, message: ${error.message}")
            }
        } else {
            log.error("No block or error response catched from Iroha: $response")
        }
    }

    private fun updatePropertiesInDatabase(
        lastBlockState: State,
        lastRequest: State
    ) {
        var newLastBlock = lastBlockState.value.toLong()
        newLastBlock++
        lastBlockState.value = newLastBlock.toString()
        stateRepo.save(lastBlockState)
        var newQueryNumber = lastRequest.value.toLong()
        newQueryNumber++
        lastRequest.value = newQueryNumber.toString()
        stateRepo.save(lastRequest)
    }

    fun irohaBlockQuery(
        lastRequest: Long,
        lastBlock: Long
    ): QryResponses.QueryResponse {
        var lastRequest1 = lastRequest
        var lastBlock1 = lastBlock

        val q = Query.builder(userId, ++lastRequest1)
            .getBlock(++lastBlock1)
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