package jp.co.soramitsu.d3.datacollector.service

import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import jp.co.soramitsu.d3.datacollector.utils.irohaBinaryKeyfromHex
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI

@Service
class BlockTaskService {


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

    private var api: IrohaAPI? = null

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

    fun getMetadata(): Pair<Long, Long> {
        var lastBlock = stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID).get().value.toLong()
        var lastRequest = stateRepo.findById(LAST_REQUEST_ROW_ID).get().value.toLong()
        return Pair(lastBlock, lastRequest)
    }
}