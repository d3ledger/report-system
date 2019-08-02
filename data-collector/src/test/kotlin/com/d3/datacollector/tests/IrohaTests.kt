package com.d3.datacollector.tests

import com.d3.datacollector.config.AppConfig.Companion.queueName
import com.d3.datacollector.engine.TestEnv
import com.d3.datacollector.model.*
import com.d3.datacollector.service.BlockTaskService
import integration.helper.ContainerHelper
import integration.helper.KGenericContainer
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.java.Utils
import jp.co.soramitsu.iroha.java.subscription.WaitForTerminalStatus
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import junit.framework.TestCase
import mu.KLogging
import org.junit.*
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.net.URI
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class IrohaTests : TestEnv() {

    @Autowired
    private lateinit var blockTaskService: BlockTaskService

    @get:Rule
    val environmentVariables = EnvironmentVariables()

    @Before
    fun setEnv() {
        environmentVariables.set("RMQ_HOST", rmq.containerIpAddress)
    }

    private val waiter = WaitForTerminalStatus()

    @Test
    @Transactional
    fun testBatchExchange() {
        blockTaskService.runService()
        // transfer 10 usd from user_a to user_b
        val transferDescription = "For pizza"
        val transferAmount = "10"
        val tx = Transaction.builder(userAId)
            .transferAsset(
                userAId, userBId,
                usd, transferDescription, transferAmount
            ).sign(userAKeypair)
            .build()

        //To test Exchange batches
        val tx1 = Transaction.builder(userAId)
            .transferAsset(
                userAId, userBId,
                usd, "Exchange", "5"
            )
            .transferAsset(
                userAId, "$exchangeBillingAccountName@$bankDomain",
                usd, "Exchange commission", "1"
            ).sign(userAKeypair)
            .build()

        val tx2 = Transaction.builder(userAId)
            .transferAsset(
                userAId, userBId,
                usd, "Exchange", "5"
            )
            .transferAsset(
                userAId, "$exchangeBillingAccountName@$bankDomain",
                usd, "Exchange commission", "1"
            ).sign(userAKeypair)
            .build()

        val batch = listOf(tx1, tx2)
        val atomicBatch = Utils.createTxAtomicBatch(batch, userAKeypair)
        irohaAPI.transactionListSync(atomicBatch)

        val txList = listOf(tx)

        val observer = inlineTransactionStatusObserver()

        for (btx in atomicBatch) {
            val hash = Utils.hash(btx)
            waiter.subscribe(irohaAPI, hash)
                .blockingSubscribe(observer)
        }

        sendTransactionsAndEnsureBlocks(irohaAPI, txList)

        val balanceUserA = getBalance(irohaAPI, userAId, userAKeypair)
        val balanceUserB = getBalance(irohaAPI, userBId, userBKeypair)

        // ensure we got correct balances
        assertEquals(68, balanceUserA)
        assertEquals(30, balanceUserB)

        assertTrue(txBatchRepo.findAll().toCollection(ArrayList()).isNotEmpty())
    }

    @Test
    @Transactional
    fun testGetAllAssets() {
        blockTaskService.runService()
        val securityKey = "secureAsset"
        val securityValue = "secureValue"

        val tx1 = Transaction.builder(securitiesUser)
            .setAccountDetail(irohaController.securityAccount, securityKey, securityValue)
            .sign(securitiesUserKeyPair)
            .build()

        val stateTxs = listOf(tx1)
        sendTransactionsAndEnsureBlocks(irohaAPI, stateTxs)

        assertEquals(1, accountDetailRepo.getAllDetailsForAccountId(irohaController.securityAccount).size)

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/iroha/asset/getAll"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, AssetsResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(
            1,
            respBody.securities.size
        )
    }

    @Test
    @Transactional
    fun testGetBlockWithIroha() {
        blockTaskService.runService()
        // transfer 10 usd from user_a to user_b
        val transferDescription = "For pizza"
        val transferAmount = "10"
        val tx = Transaction.builder(userAId)
            .transferAsset(
                userAId, userBId,
                usd, transferDescription, transferAmount
            ).sign(userAKeypair)
            .build()
        val tx2 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace("#", latticePlaceholder), "0.6")
            .sign(transaferBillingKeyPair)
            .build()
        val tx3 = Transaction.builder(custodyBillingAccountId)
            .setAccountDetail(custodyBillingAccountId, usd.replace("#", latticePlaceholder), "0.1")
            .sign(custodyKeyPair)
            .build()
        val tx4 = Transaction.builder(accountCreationBillingAccountId)
            .setAccountDetail(accountCreationBillingAccountId, usd.replace("#", latticePlaceholder), "0.2")
            .sign(accountCreationKeyPair)
            .build()
        val tx5 = Transaction.builder(exchangeBillingAccountId)
            .setAccountDetail(exchangeBillingAccountId, usd.replace("#", latticePlaceholder), "0.3")
            .sign(exchangeKeyPair)
            .build()
        val tx6 = Transaction.builder(withdrawalBillingAccountId)
            .setAccountDetail(withdrawalBillingAccountId, usd.replace("#", latticePlaceholder), "0.4")
            .sign(withdrawalKeyPair)
            .build()
        val tx7 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace("#", latticePlaceholder), "0.5")
            .sign(transaferBillingKeyPair)
            .build()
        val tx8 = Transaction.builder(custodyBillingAccountId)
            .addSignatory(custodyBillingAccountId, userAKeypair.public)
            .sign(custodyKeyPair)
            .build()
        val tx9 = Transaction.builder(custodyBillingAccountId)
            .setAccountQuorum(custodyBillingAccountId, 2)
            .sign(custodyKeyPair)
            .build()

        val stateTxs = listOf(tx, tx2, tx3, tx4, tx5, tx6, tx7, tx8, tx9)
        sendTransactionsAndEnsureBlocks(irohaAPI, stateTxs)

        val dbTrAss = ArrayList<TransferAsset>()
        dbTrAss.addAll(transferAssetRepo.findAll())
        assertEquals(1, dbTrAss.size)
        val trnsfr = dbTrAss[0]
        assertEquals(usd, trnsfr.assetId)
        assertEquals(userBId, trnsfr.destAccountId)
        assertEquals(userAId, trnsfr.srcAccountId)
        assertEquals(transferDescription, trnsfr.description)
        assertEquals(BigDecimal(transferAmount), trnsfr.amount)
        TestCase.assertNotNull(trnsfr.transaction)
        TestCase.assertNotNull(trnsfr.transaction.creatorId)
        assertEquals(false, trnsfr.transaction.rejected)

        val dbCrtAccout = ArrayList<CreateAccount>()
        dbCrtAccout.addAll(createAccountRepo.findAll())
        assertEquals(9, dbCrtAccout.size)
        dbCrtAccout.forEach {
            TestCase.assertNotNull(it.accountName)
            TestCase.assertNotNull(it.domainId)
            TestCase.assertNotNull(it.publicKey)
            TestCase.assertNotNull(it.transaction)
            TestCase.assertNotNull(it.transaction.creatorId)
            assertEquals(1, it.transaction.block?.blockNumber)
        }

        val addSignatoryList = ArrayList<AddSignatory>()
        addSignatoryList.addAll(addSignatoryRepo.findAll())
        assertEquals(1, addSignatoryList.size)

        val usdAssetId = "$usdName#$bankDomain"

        try {
            val transaferBilling = cache.getTransferFee(bankDomain, usdAssetId)
            assertEquals(BigDecimal("0.5"), transaferBilling.feeFraction)
            val custody = cache.getCustodyFee(bankDomain, usdAssetId)
            assertEquals(BigDecimal("0.1"), custody.feeFraction)
            val accountFee = cache.getAccountCreationFee(bankDomain, usdAssetId)
            assertEquals(BigDecimal("0.2"), accountFee.feeFraction)
            val exchangeFee = cache.getExchangeFee(bankDomain, usdAssetId)
            assertEquals(BigDecimal("0.3"), exchangeFee.feeFraction)
            val withdrawalFee = cache.getWithdrawalFee(bankDomain, usdAssetId)
            assertEquals(BigDecimal("0.4"), withdrawalFee.feeFraction)
            billingRepo.findAll().forEach {
                logger.info("Received asset: ${it.asset}")
                TestCase.assertTrue(it.asset.contains('#'))
                TestCase.assertFalse(it.domainName.isEmpty())
                TestCase.assertNotNull(it.billingType)
            }

            val detailList = ArrayList<SetAccountDetail>()
            detailList.addAll(accountDetailRepo.findAll())
            assertEquals(detailKey, detailList[0].detailKey)
            assertEquals(detailValue, detailValue)
        } catch (e: RuntimeException) {
            logger.error("Error getting billing", e)
            TestCase.fail()
        }
        val custodyQuorum = accountQuorumRepo.getQuorumByAccountId(custodyBillingAccountId)
        assertEquals(2, custodyQuorum.size)
        assertEquals(2, custodyQuorum[0].quorum)
    }

    @Test
    @Transactional
    fun testGetBilllingWithIroha() {
        blockTaskService.runService()
        val fee = "0.6"

        val tx1 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace("#", latticePlaceholder), fee)
            .sign(transaferBillingKeyPair)
            .build()
        val txList = listOf(tx1)
        sendTransactionsAndEnsureBlocks(irohaAPI, txList)

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, BillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(
            BigDecimal(fee),
            respBody.transfer[bankDomain]!![usd]!!.feeFraction
        )
    }

    @Test
    @Transactional
    fun testGetSingleBilllingWithIroha() {
        blockTaskService.runService()
        val fee = "0.6"

        val tx1 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace("#", latticePlaceholder), fee)
            .sign(transaferBillingKeyPair)
            .build()
        val txList = listOf(tx1)
        sendTransactionsAndEnsureBlocks(irohaAPI, txList)

        val domain = bankDomain
        val asset = usdName

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing/$domain/$asset/$bankDomain/TRANSFER"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, SingleBillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(
            BigDecimal(fee),
            respBody.billing.feeFraction
        )
    }

    companion object : KLogging() {
        private lateinit var irohaAPI: IrohaAPI

        private val iroha = IrohaContainer().withLogger(null)
        private val chainAdapter = KGenericContainer("nexus.iroha.tech:19002/d3-deploy/chain-adapter:master")
        private val rmq = ContainerHelper().rmqFixedPortContainer

        @BeforeClass
        @JvmStatic
        fun setUp() {
            iroha.withPeerConfig(peerConfig)
                .withIrohaAlias("d3-iroha")
                .start()

            rmq.withCreateContainerCmdModifier { it.withName("d3-rmq") }
                .withNetwork(iroha.network)
                .start()

            Thread.sleep(20000)

            chainAdapter
                .withEnv("CHAIN-ADAPTER_DROPLASTREADBLOCK", "true")
                .withEnv("CHAIN-ADAPTER_QUEUESTOCREATE", queueName)
                .withNetwork(iroha.network)
                .start()

            irohaAPI = IrohaAPI(URI(iroha.toriiAddress.toString()))
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            chainAdapter.stop()
            rmq.stop()
            irohaAPI.close()
            iroha.stop()
        }
    }
}
