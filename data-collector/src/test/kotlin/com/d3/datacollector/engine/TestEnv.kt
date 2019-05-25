package com.d3.datacollector.engine

import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.controllers.IrohaController
import com.d3.datacollector.repository.*
import com.d3.datacollector.service.BlockTaskService
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
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus

open class TestEnv {

    val mapper = ObjectMapper()

    @Autowired
    lateinit var mvc: MockMvc

    val irohaTxWaiter = WaitForTerminalStatus()

    @Value("\${iroha.latticePlaceholder}")
    lateinit var latticePlaceholder: String
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

    val userAId = "user_a@bank"
    val userBId = "user_b@bank"
    val securitiesUser = "assets_list@security"

    val bankDomain = "bank"
    val securityDomain = "security"
    val notaryDomain = "notary"
    val userRole = "user"
    val usdName = "usd"
    val dataCollectorRole = "dataCollector"
    val transferBillingAccountName = "transfer_billing"
    val transferBillingAccountId = "$transferBillingAccountName@$bankDomain"
    val custodyAccountName = "custody_billing"
    val custodyBillingAccountId = "$custodyAccountName@$bankDomain"
    val accountCreationBillingAccountName = "account_creation_billing"
    val accountCreationBillingAccountId = "$accountCreationBillingAccountName@$bankDomain"
    val exchangeBillingAccountName = "exchange_billing"
    val exchangeBillingAccountId = "$exchangeBillingAccountName@$bankDomain"
    val withdrawalBillingAccountName = "withdrawal_billing"
    val withdrawalBillingAccountId = "$withdrawalBillingAccountName@$bankDomain"

    val crypto = Ed25519Sha3()

    val peerKeypair = crypto.generateKeypair()

    val userAKeypair = crypto.generateKeypair()
    val userBKeypair = crypto.generateKeypair()
    val securitiesUserKeyPair = crypto.generateKeypair()
    val transaferBillingKeyPair = crypto.generateKeypair()
    val custodyKeyPair = crypto.generateKeypair()
    val accountCreationKeyPair = crypto.generateKeypair()
    val exchangeKeyPair = crypto.generateKeypair()
    val withdrawalKeyPair = crypto.generateKeypair()

    val detailKey = "bing"
    val detailValue = "bong"

    fun user(name: String): String {
        return String.format("%s@%s", name, bankDomain)
    }

    val usd = String.format(
        "%s#%s",
        usdName,
        bankDomain
    )

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
                            Primitive.RolePermission.can_receive,
                            Primitive.RolePermission.can_set_quorum,
                            Primitive.RolePermission.can_add_signatory,
                            Primitive.RolePermission.can_get_my_acc_detail
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
                    .createAccount(accountCreationBillingAccountName, bankDomain, accountCreationKeyPair.public)
                    .createAccount("data_collector", notaryDomain, Utils.parseHexPublicKey(dataCollectorPublicKey))
                    .createAccount("user_a", bankDomain, userAKeypair.public)
                    .createAccount("user_b", bankDomain, userBKeypair.public)
                    .createAccount(irohaController.assetList,securityDomain, securitiesUserKeyPair.public)
                    .createAsset(usdName, bankDomain, 2)
                    .setAccountDetail("user_a@$bankDomain", detailKey, detailValue)
                    .setAccountQuorum(custodyBillingAccountId, 1)
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

    // don't forget to add peer keypair to config
    val peerConfig: PeerConfig
        get() {
            val config = PeerConfig.builder()
                .genesisBlock(genesisBlock)
                .build()
            config.withPeerKeyPair(peerKeypair)
            return config
        }

    fun prepareState(
        api: IrohaAPI,
        txs: List<TransactionOuterClass.Transaction?>
    ) {
        val observer = inlineTransactionStatusObserver()
        // blocking send.
        // use .subscribe() for async sending
        txs.forEach {
            api.transaction(it)
                .blockingSubscribe(observer)
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
        val assetUsdOptional = assets
            .stream()
            .filter { a -> a.assetId == usd }
            .findFirst()

        // numbers are small, so we use int here for simplicity
        return assetUsdOptional
            .map { a -> Integer.parseInt(a.balance) }
            .orElse(0)
    }

    fun getBlockAndCheck(number: Long): String {
        blockTaskService.processBlockTask()
        var lastProcessedBlock = stateRepo.findById(blockTaskService.LAST_PROCESSED_BLOCK_ROW_ID).get().value
        TestCase.assertTrue(lastProcessedBlock.toLong() == number)
        return lastProcessedBlock
    }

}
