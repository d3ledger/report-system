/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.tests

import iroha.protocol.Primitive
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.TransactionStatusObserver
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import mu.KLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.security.KeyPair
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(
    properties = arrayOf(
        "app.scheduling.enable=false",
        "app.rabbitmq.enable=false"
    )
)
class DemoTest {

    private val log = KLogging().logger

    private val crypto = Ed25519Sha3()

    private val userRole = "user"
    private val bankDomain = "bank"
    private val usdName = "usd"
    private val userAName = "user_a"
    private val userBName = "user_b"

    private val userAId = "$userAName@$bankDomain"
    private val userBId = "$userBName@$bankDomain"


    private val usd = String.format(
        "%s#%s",
        usdName,
        bankDomain
    )

    @Test
    fun demoTest() {
        /* Create keypairs */
        /*val peerKeypair = crypto.generateKeypair()
        val useraKeypair = crypto.generateKeypair()
        val userbKeypair = crypto.generateKeypair()*/

        /* Create genesis block */
        /*val genesisBlock = GenesisBlockBuilder()
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
                            Primitive.RolePermission.can_add_signatory
                        )
                    )
                    .createDomain(bankDomain, userRole)
                    .createAccount(userAName, bankDomain, useraKeypair.public)
                    .createAccount(userBName, bankDomain, userbKeypair.public)
                    .createAsset(usdName, bankDomain, 2)
                    .build()
                    .build()
            )
            .addTransaction(
                Transaction.builder(userAId)
                    .addAssetQuantity(usd, BigDecimal("100"))
                    .build()
                    .build()
            )
            .build()*/

        /* Create Peer config */
        /*val peerConfig = PeerConfig.builder()
            .genesisBlock(genesisBlock)
            .build()
        peerConfig.withPeerKeyPair(peerKeypair)*/


        /* Create Iroha instance */
       /* val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)
        iroha.start()*/

        /* create API wrapper */
/*
        val api = IrohaAPI(iroha.toriiAddress)
*/

        /* Create transfer Transaction */
       /* val transferDescription = "For pizza"
        val transferAmount = "10"
        val tx = Transaction.builder("user_a@$bankDomain")
            .transferAsset(
                userAId, userBId,
                usd, transferDescription, transferAmount
            ).sign(useraKeypair)
            .build()*/

        /* Create Transaction Observer */
       /* val observer = TransactionStatusObserver.builder()
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
            .build()*/

        // blocking send.
        // use .subscribe() for async sending
    /*    api.transaction(tx)
            .blockingSubscribe(observer)*/

        /// now lets query balances
       /* val balanceUserA = getBalance(
            api,
            userAId,
            useraKeypair
        )
        val balanceUserB = getBalance(
            api,
            userBId,
            userbKeypair
        )*/

        // ensure we got correct balances
     /*   assert(balanceUserA == 90)
        assert(balanceUserB == 10)*/
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


}
