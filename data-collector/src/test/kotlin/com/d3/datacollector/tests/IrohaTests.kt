package com.d3.datacollector.tests

import com.d3.datacollector.config.AppConfig.Companion.queueName
import com.d3.datacollector.engine.TestEnv
import com.d3.datacollector.model.*
import com.d3.datacollector.service.BlockTaskService
import com.d3.datacollector.utils.gson
import com.d3.datacollector.utils.toDcBigDecimal
import com.google.gson.JsonParser
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
import java.math.BigDecimal
import java.net.URI
import java.util.*
import javax.transaction.Transactional
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
        environmentVariables.set("IROHA_TORIIADDRESS", iroha.toriiAddress.toString())
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
        val tx2 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                transferBillingAccountId,
                feeCode,
                Utils.irohaEscape(
                    gson.toJson(
                        IrohaDetailValueDTO(
                            usd,
                            "*",
                            Billing.FeeTypeEnum.FIXED.name,
                            "0.6",
                            Billing.FeeNatureEnum.SUBTRACT.name,
                            Billing.FeeComputationEnum.FEE.name,
                            "",
                            "0",
                            "100500",
                            "0",
                            "-1"
                        )
                    )
                )
            )
            .sign(adminKeyPair)
            .build()
        val custodyBillingDTO = IrohaDetailValueDTO(
            usd,
            "*",
            Billing.FeeTypeEnum.FIXED.name,
            "0.1",
            Billing.FeeNatureEnum.SUBTRACT.name,
            Billing.FeeComputationEnum.FEE.name,
            "",
            "0",
            "-1",
            "0",
            "-1"
        )
        val tx3 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                custodyBillingAccountId,
                feeCode,
                Utils.irohaEscape(gson.toJson(custodyBillingDTO))
            )
            .sign(adminKeyPair)
            .build()
        val accountCreationBillingDTO = IrohaDetailValueDTO(
            usd,
            "*",
            Billing.FeeTypeEnum.FIXED.name,
            "0.3",
            Billing.FeeNatureEnum.SUBTRACT.name,
            Billing.FeeComputationEnum.FEE.name,
            "",
            "0",
            "-1",
            "0",
            "-1"
        )
        val tx4 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                accountCreationBillingAccountId,
                feeCode,
                Utils.irohaEscape(gson.toJson(accountCreationBillingDTO))
            )
            .sign(adminKeyPair)
            .build()
        val exchangeBillingDTO = IrohaDetailValueDTO(
            usd,
            "*",
            Billing.FeeTypeEnum.FIXED.name,
            "0.2",
            Billing.FeeNatureEnum.SUBTRACT.name,
            Billing.FeeComputationEnum.FEE.name,
            "",
            "0",
            "-1",
            "0",
            "-1"
        )
        val tx5 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                exchangeBillingAccountId,
                feeCode,
                Utils.irohaEscape(gson.toJson(exchangeBillingDTO))
            )
            .sign(adminKeyPair)
            .build()
        val withdrawalBillingDTO = IrohaDetailValueDTO(
            usd,
            "*",
            Billing.FeeTypeEnum.FIXED.name,
            "0.111",
            Billing.FeeNatureEnum.SUBTRACT.name,
            Billing.FeeComputationEnum.FEE.name,
            "",
            "0",
            "-1",
            "0",
            "-1"
        )
        val tx6 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                withdrawalBillingAccountId,
                feeCode,
                Utils.irohaEscape(gson.toJson(withdrawalBillingDTO))
            )
            .sign(adminKeyPair)
            .build()
        val transferBillingDTO = IrohaDetailValueDTO(
            usd,
            "*",
            Billing.FeeTypeEnum.FIXED.name,
            "0.22",
            Billing.FeeNatureEnum.SUBTRACT.name,
            Billing.FeeComputationEnum.FEE.name,
            "",
            "0",
            "100500",
            "0",
            "-1"
        )
        val tx7 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                transferBillingAccountId,
                feeCode,
                Utils.irohaEscape(gson.toJson(transferBillingDTO))
            )
            .sign(adminKeyPair)
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
        assertEquals(11, dbCrtAccout.size)
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

        try {
            val transferBilling = cache.getTransferBilling(bankDomain, usd)
            assertTrue(
                transferBilling.any { (_, v) ->
                    v.contains(
                        transferBillingDTO.toBilling(
                            Billing.BillingTypeEnum.TRANSFER,
                            feeCode,
                            bankDomain
                        )
                    )
                }
            )
            transferBilling.forEach { (_, v) ->
                run {
                    assertEquals(
                        1,
                        v.size
                    )
                    assertTrue(
                        v.any { it.feeFraction.compareTo(BigDecimal(transferBillingDTO.feeFraction)) == 0 }
                    )
                }
            }
            val custody = cache.getCustodyBilling(bankDomain, usd)
            assertTrue(
                custody.any { (_, v) ->
                    v.contains(
                        custodyBillingDTO.toBilling(
                            Billing.BillingTypeEnum.CUSTODY,
                            feeCode,
                            bankDomain
                        )
                    )
                }
            )
            val accountFee = cache.getAccountCreationBilling(bankDomain, usd)
            assertTrue(
                accountFee.any { (_, v) ->
                    v.contains(
                        accountCreationBillingDTO.toBilling(
                            Billing.BillingTypeEnum.ACCOUNT_CREATION,
                            feeCode,
                            bankDomain
                        )
                    )
                }
            )
            val exchangeFee = cache.getExchangeBilling(bankDomain, usd)
            assertTrue(
                exchangeFee.any { (_, v) ->
                    v.contains(
                        exchangeBillingDTO.toBilling(
                            Billing.BillingTypeEnum.EXCHANGE,
                            feeCode,
                            bankDomain
                        )
                    )
                }
            )
            val withdrawalFee = cache.getWithdrawalBilling(bankDomain, usd)
            assertTrue(
                withdrawalFee.any { (_, v) ->
                    v.contains(
                        withdrawalBillingDTO.toBilling(
                            Billing.BillingTypeEnum.WITHDRAWAL,
                            feeCode,
                            bankDomain
                        )
                    )
                }
            )
            billingRepo.findAll().forEach {
                logger.info("Received asset: ${it.asset}")
                TestCase.assertTrue(it.asset.contains('#'))
                TestCase.assertFalse(it.domainName.isEmpty())
                TestCase.assertNotNull(it.billingType)
            }

            val detailList = ArrayList<SetAccountDetail>()
            detailList.addAll(accountDetailRepo.findAll())
            assertEquals(detailKey, detailList[0].detailKey)
            assertEquals(detailValue, detailList[0].detailValue)
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
        val setAccountDetailsFeeValue = IrohaDetailValueDTO(
            usd,
            "*",
            Billing.FeeTypeEnum.FIXED.name,
            "0.22",
            Billing.FeeNatureEnum.SUBTRACT.name,
            Billing.FeeComputationEnum.FEE.name,
            "",
            "0",
            "100500",
            "0",
            "-1"
        )
        val tx1 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                transferBillingAccountId,
                feeCode,
                Utils.irohaEscape(gson.toJson(setAccountDetailsFeeValue))
            )
            .sign(adminKeyPair)
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
        assertTrue(respBody.transfer[bankDomain]!![feeCode]!!.contains(setAccountDetailsFeeValue.toBilling()))
    }

    @Test
    @Transactional
    fun testGetSingleBilllingWithIroha() {
        blockTaskService.runService()
        val setAccountDetailsFeeValue = IrohaDetailValueDTO(
            usd,
            "*",
            Billing.FeeTypeEnum.FIXED.name,
            "0.22",
            Billing.FeeNatureEnum.SUBTRACT.name,
            Billing.FeeComputationEnum.FEE.name,
            "",
            "0",
            "100500",
            "0",
            "-1"
        )

        val tx1 = Transaction.builder(adminAccountId)
            .setAccountDetail(
                transferBillingAccountId,
                feeCode,
                Utils.irohaEscape(gson.toJson(setAccountDetailsFeeValue))
            )
            .sign(adminKeyPair)
            .build()
        val txList = listOf(tx1)
        sendTransactionsAndEnsureBlocks(irohaAPI, txList)

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing/$bankDomain/$usdName/$bankDomain/TRANSFER"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, SingleBillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(1, respBody.feeInfo.size)
        assertTrue(respBody.feeInfo.any { it.feeEntries.contains(setAccountDetailsFeeValue.toBilling()) })
        assertEquals(2, respBody.assetPrecision)
    }

    @Test
    @Transactional
    fun testGetSingleExchangeRateWithIroha() {
        blockTaskService.runService()
        val url =
            "https://query1.finance.yahoo.com/v7/finance/quote?formatted=true&symbols=RUB%3DX&fields=regularMarketPrice"
        val tx1 = Transaction.builder(userAId)
            .setAccountDetail(
                dataCollectorId,
                "rate_attribute",
                "quoteResponse.result.regularMarketPrice.raw"
            )
            .setAccountDetail(
                dataCollectorId,
                usd.replace("#", latticePlaceholder),
                Utils.irohaEscape(url)
            )
            .sign(userAKeypair)
            .build()
        val txList = listOf(tx1)
        sendTransactionsAndEnsureBlocks(irohaAPI, txList)

        Thread.sleep(5000)

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/rates/$usdName/$bankDomain"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val resultDecimal =
            BigDecimal(JsonParser().parse(result.response.contentAsString).asJsonObject.get("itIs").asString)
        assertTrue(resultDecimal > BigDecimal.valueOf(50))
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
                .withFixedPort(50051)
                .start()

            rmq.withNetworkAliases("d3-rmq")
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

fun IrohaDetailValueDTO.toBilling() =
    // Does not reuse toBilling with params method because of default values
    Billing(
        destination = destination,
        feeType = Billing.FeeTypeEnum.valueOf(feeType),
        feeFraction = feeFraction.toDcBigDecimal(),
        feeNature = Billing.FeeNatureEnum.valueOf(feeNature),
        feeComputation = Billing.FeeComputationEnum.valueOf(feeComputation),
        feeAccount = feeAccount,
        minAmount = minAmount.toDcBigDecimal(),
        maxAmount = maxAmount.toDcBigDecimal(),
        minFee = minFee.toDcBigDecimal(),
        maxFee = maxFee.toDcBigDecimal()
    )
