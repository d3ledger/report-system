/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.tests

import com.d3.report.model.Block
import com.d3.report.model.Transaction
import com.d3.report.model.TransferAsset
import com.d3.report.model.TransferReport
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.TransactionRepo
import com.d3.report.repository.TransferAssetRepo
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import javax.transaction.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TestTransferReport {

    @Autowired
    lateinit var transferRepo: TransferAssetRepo
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var transactionRepo: TransactionRepo

    private val mapper = ObjectMapper()

    @Autowired
    lateinit var mvc: MockMvc
    @Value("\${iroha.templates.transferBilling}")
    private lateinit var transferBillingTemplate: String

    val domain = "author"
    val otherDomain = "otherDomain"
    val accountId = "srcAcc@$domain"
    val assetId = "assetId@$domain"

    @Test
    @Transactional
    fun testTransferReportForCustomerAndAsset() {
        prepareDataForAccountAndAssetReportTest()
        val page = transferRepo.getDataBetween(
            "srcAcc@$domain",
            "${transferBillingTemplate}$domain",
            2,
            4,
            PageRequest.of(0, 5)
        )
        assertEquals(4, page.numberOfElements)
        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/account/transferAsset")
                    .param("accountId", accountId)
                    .param("from", "2")
                    .param("to", "4")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
                    .param("assetId", assetId)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, TransferReport::class.java)

        assertEquals(1, respBody.transfers.size)
        assertNotNull(respBody.transfers[0].transfer)
        assertEquals(BigDecimal("10"), respBody.transfers[0].transfer?.amount)
        assertEquals(assetId, respBody.transfers[0].transfer?.assetId)

        assertNotNull(respBody.transfers[0].fee)
        assertEquals(BigDecimal("1"), respBody.transfers[0].fee?.amount)
        assertEquals(assetId, respBody.transfers[0].fee?.assetId)
    }

    @Test
    @Transactional
    fun testTransferReportForCustomer() {
        prepareDataForAccountReportTest()
        val page = transferRepo.getDataBetween(
            "srcAcc@$domain",
            "${transferBillingTemplate}$domain",
            2,
            4,
            PageRequest.of(0, 5)
        )
        assertEquals(2, page.numberOfElements)
        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/account/transferAsset")
                    .param("accountId", accountId)
                    .param("from", "2")
                    .param("to", "4")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, TransferReport::class.java)

        assertEquals(1, respBody.transfers.size)
        assertNotNull(respBody.transfers[0].transfer)
        assertEquals(BigDecimal("10"), respBody.transfers[0].transfer?.amount)
        assertEquals(assetId, respBody.transfers[0].transfer?.assetId)

        assertNotNull(respBody.transfers[0].fee)
        assertEquals(BigDecimal("1"), respBody.transfers[0].fee?.amount)
        assertEquals(assetId, respBody.transfers[0].fee?.assetId)

    }


    private fun prepareDataForAccountAndAssetReportTest() {
        prepareDataForAccountReportTest()
        val block2 = blockRepo.findById(2)

        val transaction =
            transactionRepo.save(Transaction(null, block2.get(), "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                accountId,
                "destAcc@$domain",
                "otherAsset@$domain",
                null,
                BigDecimal("10"),
                transaction
            )
        )
        transferRepo.save(
            TransferAsset(
                accountId,
                "${transferBillingTemplate}$domain",
                "otherAsset@$domain",
                null,
                BigDecimal("1"),
                transaction
            )
        )

    }

    private fun prepareDataForAccountReportTest() {
        val block1 = blockRepo.save(Block(1, 1))
        val transaction1 =
            transactionRepo.save(Transaction(null, block1, "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                accountId,
                "destAcc@$domain",
                assetId,
                null,
                BigDecimal("1"),
                transaction1
            )
        )

        val block2 = blockRepo.save(Block(2, 2))
        val transaction2 =
            transactionRepo.save(Transaction(null, block2, "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                accountId,
                "destAcc@$domain",
                assetId,
                null,
                BigDecimal("10"),
                transaction2
            )
        )
        transferRepo.save(
            TransferAsset(
                accountId,
                "${transferBillingTemplate}$domain",
                assetId,
                null,
                BigDecimal("1"),
                transaction2
            )
        )

        val transaction21 =
            transactionRepo.save(Transaction(null, block2, "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                "otherSrc@$domain",
                "destAcc@$domain",
                assetId,
                null,
                BigDecimal("10"),
                transaction21
            )
        )
        transferRepo.save(
            TransferAsset(
                "otherSrc@$domain",
                "${transferBillingTemplate}$domain",
                assetId,
                null,
                BigDecimal("1"),
                transaction21
            )
        )

        var block3 = blockRepo.save(Block(3, 5))
        var transaction3 =
            transactionRepo.save(Transaction(null, block3, "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                accountId,
                "destAcc@$domain",
                assetId,
                null,
                BigDecimal("1"),
                transaction3
            )
        )
        transferRepo.save(
            TransferAsset(
                accountId,
                "${transferBillingTemplate}$domain",
                assetId,
                null,
                BigDecimal("1"),
                transaction3
            )
        )
    }

    @Test
    @Transactional
    fun testTransferReportForAgent() {
        prepareDataForDomainReportTest()
        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/domain/transferAsset")
                    .param("from", "1")
                    .param("to", "99999")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
                    .param("domain", domain)
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, TransferReport::class.java)

        assertEquals(1, respBody.transfers.size)
        assertNotNull(respBody.transfers[0].fee)
    }

    private fun prepareDataForDomainReportTest() {
        val block1 = blockRepo.save(Block(1, 129))
        val transaction1 =
            transactionRepo.save(Transaction(null, block1, "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                "srcAcc@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction1
            )
        )

        val block2 = blockRepo.save(Block(2, 1299))
        val transaction2 =
            transactionRepo.save(Transaction(null, block2, "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                "srcAcc@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("20"),
                transaction2
            )
        )

        val block3 = blockRepo.save(Block(3, 1398))
        val transaction3 =
            transactionRepo.save(Transaction(null, block3, "mySelf@$domain", 1, false))
        transferRepo.save(
            TransferAsset(
                "srcAcc@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("20"),
                transaction3
            )
        )

        val block4 = blockRepo.save(Block(4, 1499))
        val transaction4 =
            transactionRepo.save(Transaction(null, block4, "mySelf@$domain", 1, true))
        transferRepo.save(
            TransferAsset(
                "srcAcc@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("20"),
                transaction4
            )
        )

        transferRepo.save(
            TransferAsset(
                "srcAcc@$domain",
                "$transferBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction3
            )
        )

        // Add other domain to test that it will be not included in report
        var transaction6 =
            transactionRepo.save(Transaction(null, block3, "mySelf@$otherDomain", 1, false))

        transferRepo.save(
            TransferAsset(
                "srcAcc@$otherDomain",
                "$transferBillingTemplate$otherDomain",
                "assetId@$otherDomain",
                null,
                BigDecimal("0.2"),
                transaction6
            )
        )
        transferRepo.save(
            TransferAsset(
                "srcAcc@$otherDomain",
                "destAcc@$otherDomain",
                "assetId@$otherDomain",
                null,
                BigDecimal("2"),
                transaction6
            )
        )
    }
}
