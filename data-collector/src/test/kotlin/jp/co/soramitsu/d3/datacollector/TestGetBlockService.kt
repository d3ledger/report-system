package jp.co.soramitsu.d3.datacollector

import iroha.protocol.*
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.d3.datacollector.cache.CacheRepository
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import jp.co.soramitsu.d3.datacollector.service.BlockTaskService
import jp.co.soramitsu.d3.datacollector.utils.irohaPublicKeyFromHex
import jp.co.soramitsu.iroha.java.*
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import mu.KLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner

import java.math.BigDecimal
import java.security.KeyPair
import java.util.Arrays
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = arrayOf("app.scheduling.enable=false"))
class TestGetBlockService {
    private val log = KLogging().logger

    @Value("\${iroha.user.publicKeyHex}")
    lateinit var dataCollectorPublicKey: String
    @Autowired
    lateinit var blockTaskService: BlockTaskService
    @Autowired
    lateinit var stateRepo: StateRepository
    @Autowired
    lateinit var cache: CacheRepository

    private val bankDomain = "bank"
    private val notaryDomain = "notary"
    private val userRole = "user"
    private val usdName = "usd"
    private val dataCollectorRole = "dataCollector"
    private val transferBillingAccountName = "transfer_billing"
    private val transferBillingAccountId = "$transferBillingAccountName@$bankDomain"

    private val crypto = Ed25519Sha3()

    private val peerKeypair = crypto.generateKeypair()

    private val useraKeypair = crypto.generateKeypair()
    private val userbKeypair = crypto.generateKeypair()
    private val transaferBillingKeyPair = crypto.generateKeypair()

    private fun user(name: String): String {
        return String.format("%s@%s", name, bankDomain)
    }

    private val usd = String.format(
        "%s#%s",
        usdName,
        bankDomain
    )


    @Test
    fun testSomethingWithIroha() {
        val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)
        // start the peer. blocking call
        iroha.start()
        blockTaskService.toriiAddress = iroha.toriiAddress.toString()

        // create API wrapper
        val api = IrohaAPI(iroha.toriiAddress)

        // transfer 100 usd from user_a to user_b
        val tx = Transaction.builder("user_a@bank")
            .transferAsset(
                "user_a@bank", "user_b@bank",
                usd, "For pizza", "10"
            )
            .sign(useraKeypair)
            .build()

        val tx2 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace('#','_'), "0.5")
            .sign(transaferBillingKeyPair)
            .build()

        val observer = inlineTransactionStatusObserver()

        val (balanceUserA, balanceUserB) = checkBalancesTransactions(api, tx2, observer, tx)

        // ensure we got correct balances
        assert(balanceUserA == 90)
        assert(balanceUserB == 10)


        blockTaskService.processBlockTask()
        var lastProcessedBlock = stateRepo.findById(blockTaskService.LAST_PROCESSED_BLOCK_ROW_ID).get().value
        assertTrue(lastProcessedBlock.toLong() == 1L)

        blockTaskService.processBlockTask()
        lastProcessedBlock = stateRepo.findById(blockTaskService.LAST_PROCESSED_BLOCK_ROW_ID).get().value
        assertTrue(lastProcessedBlock.toLong() == 2L)
        val usd_ = usd.replace('#','_')
        try {
            val fee = cache.getTransferBilling(transferBillingAccountId, usd_)
            assertEquals(BigDecimal("0.5"), fee)
        } catch (e: RuntimeException) {
            fail()
        }
    }

    private fun checkBalancesTransactions(
        api: IrohaAPI,
        tx2: TransactionOuterClass.Transaction?,
        observer: InlineTransactionStatusObserver?,
        tx: TransactionOuterClass.Transaction?
    ): Pair<Int, Int> {
        api.transaction(tx2).blockingSubscribe(observer)
        // blocking send.
        // use .subscribe() for async sending
        api.transaction(tx)
            .blockingSubscribe(observer)

        /// now lets query balances
        val balanceUserA = getBalance(
            api,
            user("user_a"),
            useraKeypair
        )
        val balanceUserB = getBalance(
            api,
            user("user_b"),
            userbKeypair
        )
        return Pair(balanceUserA, balanceUserB)
    }

    /**
     * create transaction observer
     * here you can specify any kind of handlers on transaction statuses
     */
    private fun inlineTransactionStatusObserver(): InlineTransactionStatusObserver? {

        val observer = TransactionStatusObserver.builder()
            // executed when stateless or stateful validation is failed
            .onTransactionFailed { t ->
                println(
                    String.format(
                        "transaction %s failed with msg: %s",
                        t.txHash,
                        t.errOrCmdName
                    )
                )
            }
            // executed when got any exception in handlers or grpc
            .onError { e -> println("Failed with exception: $e") }
            // executed when we receive "committed" status
            .onTransactionCommitted { t -> println("Committed :)") }
            // executed when transfer is complete (failed or succeed) and observable is closed
            .onComplete { println("Complete") }
            .build()
        return observer
    }

    /**
     * Custom facade over GRPC Query
     */
    fun getBalance(api: IrohaAPI, userId: String, keyPair: KeyPair): Int {
        // build protobuf query, sign it
        val q = Query.builder(userId, 1)
            .getAccountAssets(userId)
            .buildSigned(keyPair)

        // execute query, get response
        val res = api.query(q)

        // get list of assets from our response
        val assets = res.accountAssetsResponse.accountAssetsList

        // find usd asset
        val assetUsdOptional = assets
            .stream()
            .filter { a -> a.assetId == usd }
            .findFirst()

        // numbers are small, so we use int here for simplicity
        return assetUsdOptional
            .map { a -> Integer.parseInt(a.balance) }
            .orElse(0)
    }


    // don't forget to add peer keypair to config
    val peerConfig: PeerConfig
        get() {
            val config = PeerConfig.builder()
                .genesisBlock(genesisBlock)
                .build()
            config.withPeerKeyPair(peerKeypair)
            return config
        }

    private// first transaction
    // transactions in genesis block can have no creator
    // by default peer is listening on port 10001
    // create default "user" role
    // create transferBillingAccountId role
    // create transferBillingAccountId account
    // create data_collector@notary account
    // create user A
    // create user B
    // create usd#bank with precision 2
    // transactions in genesis block can be unsigned
    // returns ipj model Transaction
    // returns unsigned protobuf Transaction
    // we want to increase user_a balance by 100 usd
    val genesisBlock: BlockOuterClass.Block
        get() = GenesisBlockBuilder()
            .addTransaction(
                Transaction.builder(null)
                    .addPeer("0.0.0.0:10001", peerKeypair.public)
                    .createRole(
                        userRole,
                        Arrays.asList<Primitive.RolePermission>(
                            Primitive.RolePermission.can_transfer,
                            Primitive.RolePermission.can_get_my_acc_ast,
                            Primitive.RolePermission.can_get_my_txs,
                            Primitive.RolePermission.can_receive
                        )
                    )
                    .createRole(
                        dataCollectorRole,
                        Arrays.asList<Primitive.RolePermission>(
                            Primitive.RolePermission.can_get_blocks
                        )
                    )
                    .createDomain(bankDomain, userRole)
                    .createDomain(notaryDomain, dataCollectorRole)
                    .createAccount(transferBillingAccountName, bankDomain, transaferBillingKeyPair.public)
                    .createAccount("data_collector", notaryDomain, irohaPublicKeyFromHex(dataCollectorPublicKey))
                    .createAccount("user_a", bankDomain, useraKeypair.public)
                    .createAccount("user_b", bankDomain, userbKeypair.public)
                    .createAsset(usdName, bankDomain, 2)
                    .build()
                    .build()
            )
            .addTransaction(
                Transaction.builder(user("user_a"))
                    .addAssetQuantity(usd, BigDecimal("100"))
                    .build()
                    .build()
            )
            .build()
}
