/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.tests

import com.d3.report.model.AssetSumReportReport
import com.d3.report.model.Block
import com.d3.report.model.Transaction
import com.d3.report.model.TransferAsset
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.TransactionRepo
import com.d3.report.repository.TransferAssetRepo
import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicInteger
import javax.transaction.Transactional

private const val BTC_PRECISION = 8

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TestAssetSumReport {

    private val blocksCounter = AtomicInteger(0)

    private val mapper = ObjectMapper()

    @Autowired
    private lateinit var mvc: MockMvc
    @Autowired
    private lateinit var transferRepo: TransferAssetRepo
    @Autowired
    private lateinit var blockRepo: BlockRepository
    @Autowired
    private lateinit var transactionRepo: TransactionRepo

    /**
     * @given Database full of transfers
     * @when sum of btc#bitcoin asset is requested
     * @then properly calculated sum of btc#bitcoin asset is returned
     */
    @Test
    @Transactional
    fun testAssetSumReport() {
        val btcAssetId = "btc#bitcoin"
        val anotherAsset = "shrek#coin"

        // Deposit BTC (+2)
        createTransfer(btcAssetId, BigDecimal(2), "notary@notary", "acc1@d3")
        // Deposit BTC (+3)
        createTransfer(btcAssetId, BigDecimal(3), "notary@notary", "acc2@d3")
        // Move money back in forth (doesn't affect)
        createTransfer(btcAssetId, BigDecimal(1), "acc1@d3", "acc2@d3")
        createTransfer(btcAssetId, BigDecimal(1), "acc2@d3", "acc1@d3")
        // Send to notary@notary (-0.5)
        createTransfer(btcAssetId, BigDecimal("0.5"), "acc1@d3", "notary@notary")
        // Pay fees (doesn't affect)
        createTransfer(btcAssetId, BigDecimal("0.05"), "acc1@d3", "billing@d3")
        // Deposit something different (doesn't affect)
        createTransfer(anotherAsset, BigDecimal(10), "notary@notary", "acc1@d3")
        // Send another asset to notary@notary (doesn't affect)
        createTransfer(anotherAsset, BigDecimal("0.5"), "acc1@d3", "notary@notary")

        // Total sum of btc#bitcoin is 2+3-0.5=4.5

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/asset/sum/iroha")
                    .param("assetId", btcAssetId)
                    .param("domainId", "d3")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, AssetSumReportReport::class.java)
        assertEquals(
            BigDecimal("4.5").setScale(BTC_PRECISION, RoundingMode.DOWN),
            respBody.totalSum.setScale(BTC_PRECISION, RoundingMode.DOWN)
        )
    }

    private fun createTransfer(assetId: String, amount: BigDecimal, srcAccountId: String, destAccountId: String) {
        val block = blockRepo.save(
            Block(
                blocksCounter.getAndIncrement().toLong(),
                System.currentTimeMillis()
            )
        )
        val transaction = transactionRepo.save(Transaction(null, block, srcAccountId, 1, false))
        transferRepo.save(
            TransferAsset(
                srcAccountId,
                destAccountId,
                assetId,
                null,
                amount,
                transaction
            )
        )
    }

}
