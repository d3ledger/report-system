package jp.co.soramitsu.d3.reportsystem.datacollector

import iroha.protocol.*
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.*
import jp.co.soramitsu.iroha.java.detail.InlineTransactionStatusObserver
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import mu.KLogging
import org.junit.BeforeClass
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.math.BigDecimal
import java.security.KeyPair
import java.util.Arrays
import java.util.Optional

class TestIrohaLib {
    private val log = KLogging().logger

    @Test
    fun testSomethingWithIroha() {
        val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)
        // start the peer. blocking call
        iroha.start()

        // create API wrapper
        val api = IrohaAPI(iroha.toriiAddress)

        // transfer 100 usd from user_a to user_b
        val tx = Transaction.builder("user_a@bank")
            .transferAsset("user_a@bank", "user_b@bank", usd, "For pizza", "10")
            .sign(useraKeypair)
            .build()

        // create transaction observer
        // here you can specify any kind of handlers on transaction statuses
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

        // blocking send.
        // use .subscribe() for async sending
        api.transaction(tx)
            .blockingSubscribe(observer)

        /// now lets query balances
        val balanceUserA = getBalance(api, user("user_a"), useraKeypair)
        val balanceUserB = getBalance(api, user("user_b"), userbKeypair)

        // ensure we got correct balances
        assert(balanceUserA == 90)
        assert(balanceUserB == 10)

        // build protobuf query, sign it
        val q = Query.builder("user_a@bank", 1)
            .getBlock(1L)
            .buildSigned(useraKeypair)

        val response = api.query(q)

        if (response.hasBlockResponse() && response.blockResponse.hasBlock()) {
            val block = response.blockResponse.block
            if (block.hasBlockV1()) {
                val a = block.blockV1
                val payload = a.payload
                val transactions = payload.transactionsList
            } else {
                log.error("Received block has no BlockV1 and no handlers for other block type in application!")
            }
            println(block.blockV1)
            block.blockV1.payload
        }

    }

    companion object {


        private val bankDomain = "bank"
        private val userRole = "user"
        private val usdName = "usd"

        private val crypto = Ed25519Sha3()

        private val peerKeypair = crypto.generateKeypair()

        private val useraKeypair = crypto.generateKeypair()
        private val userbKeypair = crypto.generateKeypair()

        private fun user(name: String): String {
            return String.format("%s@%s", name, bankDomain)
        }

        private val usd = String.format("%s#%s", usdName, bankDomain)

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
                                Primitive.RolePermission.can_receive,
                                Primitive.RolePermission.can_get_blocks
                            )
                        )
                        .createDomain(bankDomain, userRole)
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
}
