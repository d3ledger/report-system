package com.d3.datacollector.engine

import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.controllers.IrohaController
import com.d3.datacollector.repository.*
import com.d3.datacollector.service.DbService
import com.fasterxml.jackson.databind.ObjectMapper
import iroha.protocol.BlockOuterClass
import iroha.protocol.Primitive
import iroha.protocol.TransactionOuterClass
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.*
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import junit.framework.TestCase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal
import java.security.KeyPair
import java.util.*

open class TestEnv {

    companion object {
        private const val userAName = "user_a"
        private const val userBName = "user_b"
        private const val dataCollectorName = "data_collector"
        const val bankDomain = "bank"
        const val notaryDomain = "notary"
        const val userAId = "$userAName@$bankDomain"
        const val userBId = "$userBName@$bankDomain"
        const val dataCollectorId = "$dataCollectorName@$notaryDomain"
        const val securitiesUser = "assets_list@security"

        const val securityDomain = "security"
        const val userRole = "user"
        const val usdName = "usd"
        const val dataCollectorRole = "dataCollector"
        const val transferBillingAccountName = "transfer_billing"
        const val transferBillingAccountId = "$transferBillingAccountName@$bankDomain"
        const val custodyAccountName = "custody_billing"
        const val custodyBillingAccountId = "$custodyAccountName@$bankDomain"
        const val accountCreationBillingAccountName = "account_creation_billing"
        const val accountCreationBillingAccountId = "$accountCreationBillingAccountName@$bankDomain"
        const val exchangeBillingAccountName = "exchange_billing"
        const val exchangeBillingAccountId = "$exchangeBillingAccountName@$bankDomain"
        const val withdrawalBillingAccountName = "withdrawal_billing"
        const val withdrawalBillingAccountId = "$withdrawalBillingAccountName@$bankDomain"

        private val crypto = Ed25519Sha3()

        private val peerKeypair = crypto.generateKeypair()!!

        val userAKeypair = crypto.generateKeypair()!!
        val userBKeypair = crypto.generateKeypair()!!
        val securitiesUserKeyPair = crypto.generateKeypair()!!
        val transaferBillingKeyPair = crypto.generateKeypair()!!
        val custodyKeyPair = crypto.generateKeypair()!!
        val accountCreationKeyPair = crypto.generateKeypair()!!
        val exchangeKeyPair = crypto.generateKeypair()!!
        val withdrawalKeyPair = crypto.generateKeypair()!!

        const val detailKey = "bing"
        const val detailValue = "bong"

        private fun user(name: String): String {
            return String.format("%s@%s", name, bankDomain)
        }

        val usd = String.format(
            "%s#%s",
            usdName,
            bankDomain
        )

        private val genesisBlock: BlockOuterClass.Block
            get() {
                return GenesisBlockBuilder()
                    .addTransaction(
                        Transaction.builder(null)
                            .addPeer("0.0.0.0:10001", peerKeypair.public)
                            .createRole(
                                userRole,
                                Arrays.asList<Primitive.RolePermission>(
                                    Primitive.RolePermission.can_transfer,
                                    Primitive.RolePermission.can_get_my_acc_ast,
                                    Primitive.RolePermission.can_get_my_txs,
                                    Primitive.RolePermission.can_receive,
                                    Primitive.RolePermission.can_set_quorum,
                                    Primitive.RolePermission.can_add_signatory,
                                    Primitive.RolePermission.can_get_my_acc_detail,
                                    Primitive.RolePermission.can_set_detail
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
                            .createDomain(securityDomain, userRole)
                            .createAccount(transferBillingAccountName, bankDomain, transaferBillingKeyPair.public)
                            .createAccount(custodyAccountName, bankDomain, custodyKeyPair.public)
                            .createAccount(exchangeBillingAccountName, bankDomain, exchangeKeyPair.public)
                            .createAccount(withdrawalBillingAccountName, bankDomain, withdrawalKeyPair.public)
                            .createAccount(
                                accountCreationBillingAccountName,
                                bankDomain,
                                accountCreationKeyPair.public
                            )
                            .createAccount(
                                "rmq",
                                notaryDomain,
                                Utils.parseHexPublicKey("7a4af859a775dd7c7b4024c97c8118f0280455b8135f6f41422101f0397e0fa5")
                            )
                            .createAccount(
                                dataCollectorName,
                                notaryDomain,
                                Utils.parseHexPublicKey("97B888554684FB30F29FAB92991AB7ECEFFFE433AB3AF501E5DEAEE69392518C")
                            )
                            .createAccount(userAName, bankDomain, userAKeypair.public)
                            .createAccount(userBName, bankDomain, userBKeypair.public)
                            .createAccount("assets_list", securityDomain, securitiesUserKeyPair.public)
                            .createAsset(usdName, bankDomain, 2)
                            .setAccountDetail("$userAName@$bankDomain", detailKey, detailValue)
                            .setAccountQuorum(custodyBillingAccountId, 1)
                            .build()
                            .build()
                    )
                    .addTransaction(
                        Transaction.builder(user(userAName))
                            .addAssetQuantity(usd, BigDecimal("100"))
                            .build()
                            .build()
                    )
                    .build()
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
    }

    val mapper = ObjectMapper()

    @Autowired
    lateinit var mvc: MockMvc

    @Value("\${iroha.latticePlaceholder}")
    lateinit var latticePlaceholder: String
    @Autowired
    lateinit var dbService: DbService
    @Autowired
    lateinit var cache: CacheRepository
    @Autowired
    lateinit var billingRepo: BillingRepository
    @Autowired
    lateinit var transferAssetRepo: TransferAssetRepo
    @Autowired
    lateinit var createAccountRepo: CreateAccountRepo
    @Autowired
    lateinit var accountDetailRepo: SetAccountDetailRepo
    @Autowired
    lateinit var accountQuorumRepo: SetAccountQuorumRepo
    @Autowired
    lateinit var addSignatoryRepo: AddSignatoryRepository
    @Autowired
    lateinit var txBatchRepo: TransactionBatchRepo
    @Autowired
    lateinit var irohaController: IrohaController

    fun sendTransactionsAndEnsureBlocks(
        api: IrohaAPI,
        txs: List<TransactionOuterClass.Transaction?>
    ) {
        val observer = inlineTransactionStatusObserver()
        // blocking send.
        // use .subscribe() for async sending
        txs.forEach {
            val lastBlock = dbService.getLastBlockProcessedHeight()
            api.transaction(it).blockingSubscribe(observer)
            Thread.sleep(5000)
            getBlockAndCheck(lastBlock + 1)
        }
    }

    /**
     * create transaction observer
     * here you can specify any kind of handlers on transaction statuses
     */
    fun inlineTransactionStatusObserver(): InlineTransactionStatusObserver? {

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
        val assetUsdOptional = assets.firstOrNull { a -> a.assetId == usd }

        // numbers are small, so we use int here for simplicity
        return if (assetUsdOptional == null) 0 else Integer.parseInt(assetUsdOptional.balance)
    }

    private fun getBlockAndCheck(number: Long): Long {
        val lastProcessedBlock = dbService.getLastBlockProcessedHeight()
        TestCase.assertTrue(number <= lastProcessedBlock)
        return lastProcessedBlock
    }
}
