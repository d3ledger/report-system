package com.d3.datacollector.tests

import iroha.protocol.*
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.repository.BillingRepository
import com.d3.datacollector.repository.StateRepository
import com.d3.datacollector.service.BlockTaskService
import jp.co.soramitsu.iroha.java.*
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import junit.framework.TestCase.*
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
import javax.transaction.Transactional
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = arrayOf("app.scheduling.enable=false", "app.rabbitmq.enable=false"))
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
    @Autowired
    lateinit var billingRepo: BillingRepository

    private val bankDomain = "bank"
    private val notaryDomain = "notary"
    private val userRole = "user"
    private val usdName = "usd"
    private val dataCollectorRole = "dataCollector"
    private val transferBillingAccountName = "transfer_billing"
    private val transferBillingAccountId = "$transferBillingAccountName@$bankDomain"
    private val custodyAccountName = "custody_billing"
    private val custodyBillingAccountId = "$custodyAccountName@$bankDomain"
    private val accountCreationBillingAccountName = "account_creation_billing"
    private val accountCreationBillingAccountId = "$accountCreationBillingAccountName@$bankDomain"
    private val exchangeBillingAccountName = "exchange_billing"
    private val exchangeBillingAccountId = "$exchangeBillingAccountName@$bankDomain"
    private val withdrawalBillingAccountName = "withdrawal_billing"
    private val withdrawalBillingAccountId = "$withdrawalBillingAccountName@$bankDomain"

    private val crypto = Ed25519Sha3()

    private val peerKeypair = crypto.generateKeypair()

    private val useraKeypair = crypto.generateKeypair()
    private val userbKeypair = crypto.generateKeypair()
    private val transaferBillingKeyPair = crypto.generateKeypair()
    private val custodyKeyPair = crypto.generateKeypair()
    private val accountCreationKeyPair = crypto.generateKeypair()
    private val exchangeKeyPair = crypto.generateKeypair()
    private val withdrawalKeyPair = crypto.generateKeypair()

    private fun user(name: String): String {
        return String.format("%s@%s", name, bankDomain)
    }

    private val usd = String.format(
        "%s#%s",
        usdName,
        bankDomain
    )

    @Test
    @Transactional
    fun testGetBlockWithIroha() {
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
            .setAccountDetail(transferBillingAccountId, usd.replace('#','_'), "0.6")
            .sign(transaferBillingKeyPair)
            .build()
        val tx3 = Transaction.builder(custodyBillingAccountId)
            .setAccountDetail(custodyBillingAccountId, usd.replace('#','_'), "0.1")
            .sign(custodyKeyPair)
            .build()
        val tx4 = Transaction.builder(accountCreationBillingAccountId)
            .setAccountDetail(accountCreationBillingAccountId, usd.replace('#','_'), "0.2")
            .sign(accountCreationKeyPair)
            .build()
        val tx5 = Transaction.builder(exchangeBillingAccountId)
            .setAccountDetail(exchangeBillingAccountId, usd.replace('#','_'), "0.3")
            .sign(exchangeKeyPair)
            .build()
        val tx6 = Transaction.builder(withdrawalBillingAccountId)
            .setAccountDetail(withdrawalBillingAccountId, usd.replace('#','_'), "0.4")
            .sign(withdrawalKeyPair)
            .build()
        val tx7 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace('#','_'), "0.5")
            .sign(transaferBillingKeyPair)
            .build()

        prepareState(api, listOf(tx,tx2,tx3,tx4,tx5,tx6,tx7))

        for(i in 1L..8L) {
            getBlockAndCheck(i)
        }


        try {
            val transaferBilling = cache.getTransferFee(bankDomain, usd)
            assertEquals(BigDecimal("0.5"), transaferBilling.feeFraction)
            val custody = cache.getCustodyFee(bankDomain, usd)
            assertEquals(BigDecimal("0.1"), custody.feeFraction)
            val accountFee = cache.getAccountCreationFee(bankDomain, usd)
            assertEquals(BigDecimal("0.2"), accountFee.feeFraction)
            val exchangeFee = cache.getExchangeFee(bankDomain, usd)
            assertEquals(BigDecimal("0.3"), exchangeFee.feeFraction)
            val withdrawalFee = cache.getWithdrawalFee(bankDomain, usd)
            assertEquals(BigDecimal("0.4"), withdrawalFee.feeFraction)
            billingRepo.findAll().forEach {
                log.info("Received asset: ${it.asset}")
                assertTrue(it.asset.contains('#'))
                assertFalse(it.accountId.isNullOrEmpty())
                assertNotNull(it.billingType)
            }
        } catch (e: RuntimeException) {
            log.error("Error getting billing",e)
            fail()
        }
    }

    private fun getBlockAndCheck(number: Long): String {
        blockTaskService.processBlockTask()
        var lastProcessedBlock = stateRepo.findById(blockTaskService.LAST_PROCESSED_BLOCK_ROW_ID).get().value
        assertTrue(lastProcessedBlock.toLong() == number)
        return lastProcessedBlock
    }

    private fun prepareState(
        api: IrohaAPI,
        txs: List<TransactionOuterClass.Transaction?>
    ) {
        val observer = inlineTransactionStatusObserver()
        // blocking send.
        // use .subscribe() for async sending
        txs.forEach { api.transaction(it).blockingSubscribe(observer) }

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
        // ensure we got correct balances
        assert(balanceUserA == 90)
        assert(balanceUserB == 10)
    }

    /**
     * create transaction observer
     * here you can specify any kind of handlers on transaction statuses
     */
    private fun inlineTransactionStatusObserver(): InlineTransactionStatusObserver? {

        return TransactionStatusObserver.builder()
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
            .onTransactionCommitted { println("Committed :)") }
            // executed when transfer is complete (failed or succeed) and observable is closed
            .onComplete { println("Complete") }
            .build()
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

    private val genesisBlock: BlockOuterClass.Block
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
                    .createAccount(custodyAccountName,bankDomain,custodyKeyPair.public)
                    .createAccount(exchangeBillingAccountName,bankDomain,exchangeKeyPair.public)
                    .createAccount(withdrawalBillingAccountName,bankDomain,withdrawalKeyPair.public)
                    .createAccount(accountCreationBillingAccountName,bankDomain,accountCreationKeyPair.public)
                    .createAccount("data_collector", notaryDomain, Utils.parseHexPublicKey(dataCollectorPublicKey))
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
