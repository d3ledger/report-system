package com.d3.datacollector

import com.d3.datacollector.engine.TestEnv
import com.d3.datacollector.model.*
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import junit.framework.TestCase
import mu.KLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import javax.transaction.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = arrayOf("app.scheduling.enable=false", "app.rabbitmq.enable=false"))
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IrohaTests : TestEnv() {

    private val log = KLogging().logger

    @Test
    @Transactional
    fun testGetBlockWithIroha() {
        val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)
        // start the peer. blocking call
        iroha.start()
        blockTaskService.irohaService.toriiAddress = iroha.toriiAddress.toString()

        // create API wrapper
        val api = IrohaAPI(iroha.toriiAddress)

        // transfer 100 usd from user_a to user_b
        val userAId = "user_a@bank"
        val userBId = "user_b@bank"
        val transferDescription = "For pizza"
        val transferAmount = "10"
        val tx = Transaction.builder(userAId)
            .transferAsset(
                userAId, userBId,
                usd, transferDescription, transferAmount
            ).sign(useraKeypair)
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
            .addSignatory(custodyBillingAccountId, useraKeypair.public)
            .sign(custodyKeyPair)
            .build()
        val tx9 = Transaction.builder(custodyBillingAccountId)
            .setAccountQuorum(custodyBillingAccountId, 2)
            .sign(custodyKeyPair)
            .build()
        val stateTxs = listOf(tx, tx2, tx3, tx4, tx5, tx6, tx7, tx8, tx9)
        prepareState(api, stateTxs)

        for (i in 1L..stateTxs.size + 1) {
            getBlockAndCheck(i)
        }

        val dbTrAss = ArrayList<TransferAsset>()
        dbTrAss.addAll(transferAssetRepo.findAll())
        assertEquals(1, dbTrAss.size)
        val trnsfr = dbTrAss.get(0)
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
        assertEquals(8, dbCrtAccout.size)
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
            val transaferBilling = cache.getTransferFee(bankDomain, usdName)
            assertEquals(BigDecimal("0.5"), transaferBilling.feeFraction)
            val custody = cache.getCustodyFee(bankDomain, usdName)
            assertEquals(BigDecimal("0.1"), custody.feeFraction)
            val accountFee = cache.getAccountCreationFee(bankDomain, usdName)
            assertEquals(BigDecimal("0.2"), accountFee.feeFraction)
            val exchangeFee = cache.getExchangeFee(bankDomain, usdName)
            assertEquals(BigDecimal("0.3"), exchangeFee.feeFraction)
            val withdrawalFee = cache.getWithdrawalFee(bankDomain, usdName)
            assertEquals(BigDecimal("0.4"), withdrawalFee.feeFraction)
            billingRepo.findAll().forEach {
                log.info("Received asset: ${it.asset}")
                TestCase.assertTrue(it.asset.contains('#'))
                TestCase.assertFalse(it.accountId.isNullOrEmpty())
                TestCase.assertNotNull(it.billingType)
            }
            val detailList = ArrayList<SetAccountDetail>()
            detailList.addAll(accountDetailRepo.findAll())
            assertEquals(detailKey, detailList[0].detailKey)
            assertEquals(detailValue, detailValue)
        } catch (e: RuntimeException) {
            log.error("Error getting billing", e)
            TestCase.fail()
        }
        val custodyQuorum = accountQuorumRepo.getQuorumByAccountId(custodyBillingAccountId)
        assertEquals(2, custodyQuorum.size)
        assertEquals(2, custodyQuorum[0].quorum)
        iroha.stop()
    }

    @Test
    @Transactional
    fun testGetBilllingWithIroha() {
        val fee = "0.6"
        val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)
        // start the peer. blocking call
        iroha.start()
        blockTaskService.irohaService.toriiAddress = iroha.toriiAddress.toString()

        // create API wrapper
        val api = IrohaAPI(iroha.toriiAddress)

        val tx1 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace("#", latticePlaceholder), fee)
            .sign(transaferBillingKeyPair)
            .build()
        val txList = listOf(tx1)
        prepareState(api, txList)

        for (i in 1L..txList.size + 1) {
            getBlockAndCheck(i)
        }

        var result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, BillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(
            BigDecimal(fee),
            respBody.transfer[bankDomain]!![usd]!!.feeFraction
        )
        iroha.stop()
    }

    @Test
    @Transactional
    fun testGetSingleBilllingWithIroha() {
        val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)
        // start the peer. blocking call
        iroha.start()
        blockTaskService.irohaService.toriiAddress = iroha.toriiAddress.toString()
        val fee = "0.6"
        // create API wrapper
        val api = IrohaAPI(iroha.toriiAddress)

        val tx1 = Transaction.builder(transferBillingAccountId)
            .setAccountDetail(transferBillingAccountId, usd.replace("#", latticePlaceholder), fee)
            .sign(transaferBillingKeyPair)
            .build()
        val txList = listOf(tx1)
        prepareState(api, txList)

        for (i in 1L..txList.size + 1) {
            getBlockAndCheck(i)
        }

        val domain = bankDomain
        val asset = usdName

        var result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing/$domain/$asset/TRANSFER"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, SingleBillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(
            BigDecimal(fee),
            respBody.billing.feeFraction
        )
        iroha.stop()
    }
}
