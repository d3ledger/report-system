package jp.co.soramitsu.d3.datacollector.tasks

import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.d3.datacollector.model.State
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import jp.co.soramitsu.d3.datacollector.utils.irohaBinaryKeyfromHex
import jp.co.soramitsu.d3.datacollector.utils.irohaPrivateKeyFromHex
import jp.co.soramitsu.d3.datacollector.utils.irohaPublicKeyFromHex
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI
import java.util.*

@Component
class ScheduledTasks {
    val log = LoggerFactory.getLogger(ScheduledTasks::class.java)

    @Autowired
    lateinit var stateRepo: StateRepository

    @Value("\${iroha.toriiAddress}")
    lateinit var toriiAddress: String

    @Value("\${iroha.user.privateKeyHex}")
    lateinit var privateKey: String
    @Value("\${iroha.user.publicKeyHex}")
    lateinit var publicKey: String
    @Value("iroha.user.id")
    lateinit var userId: String

    val LAST_PROCESSED_BLOCK_ROW_ID = 0L
    val LAST_REQUEST_ROW_ID = 1L

    @Scheduled(fixedDelayString = "\${scheduling.iroha.block.request}", initialDelay = 5000)
    fun processBlock() {
        log.debug("Block downloading job started")

        var (lastBlock, lastRequest) = getMetadata()

        val response = IrohaBlockQuery(lastRequest, lastBlock)

        if(response.hasBlockResponse()) {

        } else if(response.hasErrorResponse()) {

        }

        log.debug("Block downloading job finished")
    }

    private fun IrohaBlockQuery(
        lastRequest: Long,
        lastBlock: Long
    ): QryResponses.QueryResponse {
        var lastRequest1 = lastRequest
        var lastBlock1 = lastBlock
        val api = IrohaAPI(URI(toriiAddress))

        val q = Query.builder(userId, ++lastRequest1)
            .getBlock(++lastBlock1)
            .buildSigned(
                Ed25519Sha3.keyPairFromBytes(
                    irohaBinaryKeyfromHex(privateKey),
                    irohaBinaryKeyfromHex(publicKey)
                )
            )

        val response = api.query(q)
        return response
    }

    private fun getMetadata(): Pair<Long, Long> {
        var lastBlock = stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID).get().value.toLong()
        var lastRequest = stateRepo.findById(LAST_REQUEST_ROW_ID).get().value.toLong()
        return Pair(lastBlock, lastRequest)
    }


}