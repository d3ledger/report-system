/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.tests

import com.d3.report.model.AssetCustodyContext
import com.d3.report.model.*
import com.d3.report.repository.*
import com.d3.report.service.CustodyService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.math.RoundingMode
import javax.transaction.Transactional
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TestCustodyReport {

    private val mapper = ObjectMapper()

    @Value("\${iroha.templates.custodyBilling}")
    private var custodyBillingTemplate = "custody_billing@"

    @Value("\${billing.custody.period}")
    private var custodyPeriod: Long = 86400000

    @Autowired
    lateinit var mvc: MockMvc
    @Autowired
    lateinit var transferRepo: TransferAssetRepo
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var transactionRepo: TransactionRepo
    @Autowired
    lateinit var accountRepo: CreateAccountRepo
    @Autowired
    lateinit var billingRepo: BillingRepository
    @Autowired
    lateinit var custodyService: CustodyService

    val testDomain = "test_domain"
    val otherDomain = "other_domain"
    val accountOne = "account1"
    val accountOneId = "$accountOne@$testDomain"
    val otherAccountId = "$accountOne@$otherDomain"
    val assetId = "assetOne@$otherDomain"
    val oneDay = 86400000
    val twoDays = 172800002
    val threeDays = 259200000

    /**
     * @given accounts it two domains with transfers. And some transfers without account which should be ignored
     * @when custody report calculated for two days
     * @then fee should be equal fee of two days and two accouts from different domains should be returned
     */
    @Test
    @Transactional
    fun testCustodyFeeReportDataForSystem() {
        prepeareData()

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/custody/system")
                    .param("to", (threeDays).toString())
                    .param("from", oneDay.toString())
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, CustodyReport::class.java)
        assertEquals(2, respBody.accounts.size)
        assertEquals(otherAccountId, respBody.accounts[0].accountId)
        assertEquals(accountOneId, respBody.accounts[1].accountId)
        assertEquals(1, respBody.accounts[0].assetCustody.size)
        assertEquals(1, respBody.accounts[1].assetCustody.size)
        assertEquals(
            BigDecimal("1.0").setScale(1),
            respBody.accounts[0].assetCustody.get(assetId)!!.setScale(1, RoundingMode.HALF_UP)
        )
        assertEquals(
            BigDecimal("1.5").setScale(8),
            respBody.accounts[1].assetCustody.get(assetId)!!.setScale(8, RoundingMode.HALF_UP)
        )
    }

    /**
     * @given accounts with transfers
     * @when custody report calculated for two days
     * @then fee should be equal fee of two days
     */
    @Test
    @Transactional
    fun testCustodyFeeReportDataCustomer() {
        prepeareData()

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/custody/customer")
                    .param("accountId", accountOneId)
                    .param("to", (threeDays).toString())
                    .param("from", oneDay.toString())
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, CustodyReport::class.java)
        assertEquals(1, respBody.accounts.size)
        assertEquals(accountOneId, respBody.accounts[0].accountId)
        assertEquals(1, respBody.accounts[0].assetCustody.size)
        assertEquals(
            BigDecimal("1.5").setScale(8),
            respBody.accounts[0].assetCustody.get(assetId)!!.setScale(8)
        )
    }

    /**
     * @given no data
     * @when custody report calculated
     * @then No records are received with status 200
     */
    @Test
    @Transactional
    fun testCustodyFeeReportEmpty() {
        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/custody/agent")
                    .param("domain", "test_domain")
                    .param("to", "99")
                    .param("from", "0")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, CustodyReport::class.java)
        assertEquals(0, respBody.accounts.size)
    }

    /**
     * @given account with one transfer
     * @when custody report calculated for two days
     * @then fee should be equal fee of two days
     */
    @Test
    @Transactional
    fun testCustodyFeeReportData() {
        prepeareData()

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/custody/agent")
                    .param("domain", testDomain)
                    .param("to", (threeDays).toString())
                    .param("from", oneDay.toString())
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, CustodyReport::class.java)
        assertEquals(1, respBody.accounts.size)
        assertEquals(accountOneId, respBody.accounts[0].accountId)
        assertEquals(1, respBody.accounts[0].assetCustody.size)
        assertEquals(
            BigDecimal("1.5").setScale(8),
            respBody.accounts[0].assetCustody.get(assetId)!!.setScale(8, RoundingMode.HALF_UP)
        )
    }

    private fun prepeareData() {
        billingRepo.save(
            Billing(
                accountId = "$custodyBillingTemplate@$testDomain",
                billingType = Billing.BillingTypeEnum.CUSTODY,
                asset = assetId,
                feeFraction = BigDecimal("0.1")
            )
        )

        prepearBlockOneWithAccounts()
        prepareBlockTwoWithTransfersBeforePeriod()
        prepareBlockThreeWithTransfersInPeriod()
        prepareBlockFourWithTransfersAfterPeriod()
    }

    private fun prepareBlockFourWithTransfersAfterPeriod() {
        var block = blockRepo.save(
            Block(
                4,
                (Integer.valueOf(threeDays) + 10).toLong()
            )
        )

        val transaction = transactionRepo.save(Transaction(null, block, accountOneId, 1, false))
        // trasfer input to used account
        transferRepo.save(
            TransferAsset(
                "not_analysed_account@domainId",
                accountOneId,
                assetId,
                null,
                BigDecimal("5"),
                transaction
            )
        )
    }

    private fun prepareBlockThreeWithTransfersInPeriod() {
        var block = blockRepo.save(
            Block(
                3,
                (Integer.valueOf(twoDays - 2)).toLong()
            )
        )

        val transaction = transactionRepo.save(Transaction(null, block, accountOneId, 1, false))
        // trasfer input to used account
        transferRepo.save(
            TransferAsset(
                "not_analysed_account@domainId",
                accountOneId,
                assetId,
                null,
                BigDecimal("5"),
                transaction
            )
        )
    }

    private fun prepareBlockTwoWithTransfersBeforePeriod() {
        var block2 = Block(
            2,
            2L
        )
        block2 = blockRepo.save(block2)

        val transaction1 = transactionRepo.save(Transaction(null, block2, accountOneId, 1, false))
        // trasfer input to used account
        transferRepo.save(
            TransferAsset(
                otherAccountId,
                accountOneId,
                assetId,
                null,
                BigDecimal("5"),
                transaction1
            )
        )
        // transfer output from used account
        transferRepo.save(
            TransferAsset(
                accountOneId,
                otherAccountId,
                assetId,
                null,
                BigDecimal("5"),
                transaction1
            )
        )
    }

    private fun prepearBlockOneWithAccounts() {
        var block1 = Block(
            1,
            1
        )
        block1 = blockRepo.save(block1)
        var transaction1 = Transaction(
            null,
            block1,
            "mySelf@$testDomain",
            1,
            false
        )
        transaction1 = transactionRepo.save(transaction1)
        /**
         * Account with testDomain to be added in to report
         */
        accountRepo.save(
            CreateAccount(
                accountOne,
                testDomain,
                "publicKey1",
                transaction1
            )
        )
        /**
         * Account with other domain not to be added in report
         */
        accountRepo.save(
            CreateAccount(
                accountOne,
                otherDomain,
                "publicKeyNotTest",
                transaction1
            )
        )

        transferRepo.save(
            TransferAsset(
                "not_analysed_account@domainId",
                otherAccountId,
                assetId,
                null,
                BigDecimal("10"),
                transaction1
            )
        )

        transferRepo.save(
            TransferAsset(
                otherAccountId,
                accountOneId,
                assetId,
                null,
                BigDecimal("5"),
                transaction1
            )
        )
    }

    @Test
    fun testAddFeePortion() {
        val assetCustodyContext = AssetCustodyContext(
            BigDecimal("0"),
            0,
            BigDecimal(10)
        )
        custodyService.addFeePortion(assetCustodyContext, custodyPeriod * 2, BigDecimal("0.1"))

        assertEquals(BigDecimal("2"), assetCustodyContext.commulativeFeeAmount.setScale(0))
    }
}
