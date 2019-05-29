package com.d3.report.tests

import com.d3.report.dto.ExchangeReportDto
import com.d3.report.model.*
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.TransactionBatchRepo
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
import java.util.stream.Collectors
import javax.transaction.Transactional
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TestExchangeReport {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var transactionBatchRepo: TransactionBatchRepo
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var txRepo: TransactionRepo
    @Autowired
    lateinit var transferRepo: TransferAssetRepo
    @Value("\${iroha.templates.exchangeBilling}")
    private lateinit var exchangeBillingTemplate: String

    private val mapper = ObjectMapper()

    val domain = "oneTest"

    /**
     * @given Some transaction batches in DB
     * @when test that report request works as expected
     * @then Should return only all exchange batches for domain in specified period
     * Parameters to test
     *
     */
    @Test
    @Transactional
    fun testGetExchangeReportForCustomer() {
        prepareData()

        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/exchange/customer")
                    .param("accountId", "mySelf@$domain")
                    .param("from", "1")
                    .param("to", "3")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, ExchangeReportDto::class.java)

        assertEquals(1, respBody.batches.size)
        val batch = respBody.batches[0]
        assertEquals(2, batch.transactions.size)
        batch.transactions.forEach {
            assertEquals(2, it.commands.size)
        }

        val tx1 = batch.transactions[0]
        val tx2 = batch.transactions[1]

        assertEquals("mySelf@$domain", tx1.creatorId)
        assertEquals("hisSelf@$domain", tx2.creatorId)

    }

    /**
     * @given Some transaction batches in DB
     * @when test that report request works as expected
     * @then Should return only all exchange batches for domain in specified period
     * Parameters to test
     *
     */
    @Test
    @Transactional
    fun testGetExchangeReportForDomain() {
        prepareData()

        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/exchange/domain")
                    .param("domain", domain)
                    .param("from", "1")
                    .param("to", "3")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, ExchangeReportDto::class.java)

        assertEquals(2, respBody.batches.size)
        val batch = respBody.batches[0]
        assertEquals(2, batch.transactions.size)
        batch.transactions.forEach {
            assertEquals(2, it.commands.size)
        }

        val tx1 = batch.transactions[0]
        val tx2 = batch.transactions[1]

        assertEquals("mySelf@$domain", tx1.creatorId)
        assertEquals("hisSelf@$domain", tx2.creatorId)

    }

    /**
     * @given Some transaction batches in DB
     * @when test that report request works as expected
     * @then Should return all exchange batches for the system in specified period
     * Parameters to test
     *
     */
    @Test
    @Transactional
    fun testGetExchangeReportForTheSystem() {
        prepareData()

        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/exchange/system")
                    .param("from", "1")
                    .param("to", "3")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, ExchangeReportDto::class.java)

        assertEquals(3, respBody.batches.size)
        val batch = respBody.batches[0]
        assertEquals(2, batch.transactions.size)
        batch.transactions.forEach {
            assertEquals(2, it.commands.size)
        }

        val tx1 = batch.transactions[0]
        val tx2 = batch.transactions[1]

        assertEquals("mySelf@$domain", tx1.creatorId)
        assertEquals("hisSelf@$domain", tx2.creatorId)

    }

    private fun prepareData() {
        prepareDataBlock0()
        prepareDataBlock1()
        prepareDataBlock4()
    }

    private fun prepareDataBlock4() {
        val block = blockRepo.save(Block(4, 4))
        val batch = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )

        var transaction0 = txRepo.save(Transaction(null, block, "mySelf@$domain", 1, false, batch = batch))
        var transaction1 = txRepo.save(Transaction(null, block, "hisSelf@$domain", 1, false, batch = batch))

        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction0
            )
        )
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction0
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction1
            )
        )
        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction1
            )
        )
    }

    private fun prepareDataBlock1() {
        val block0 = blockRepo.save(Block(1, 1))
        val batch0 = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )
        val batch1 = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )
        val batch2 = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )

        var transaction0 = txRepo.save(Transaction(null, block0, "mySelf@$domain", 1, false, batch = batch0))
        var transaction1 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false, batch = batch0))
        var transaction2 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false))
        var transaction3 = txRepo.save(Transaction(null, block0, "otherSelf@$domain", 1, false, batch = batch1))
        var transaction4 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false, batch = batch1))
        var transaction5 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false, batch = batch2))
        var transaction6 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false, batch = batch2))



//Batched transactions
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "hisSelf@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction0
            )
        )
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction0
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction1
            )
        )
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction1
            )
        )
//Transaction without batch
        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "AnyAccount$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction2
            )
        )
//Batched transactions for other user
        transferRepo.save(
            TransferAsset(
                "otherSelf@$domain",
                "hisSelf@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction3
            )
        )
        transferRepo.save(
            TransferAsset(
                "otherSelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction3
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "otherSelf@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction4
            )
        )
        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction4
            )
        )

        //Batched transactions for users in other domain
        transferRepo.save(
            TransferAsset(
                "otherSelf@otherDomain",
                "hisSelf@otherDomain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction5
            )
        )
        transferRepo.save(
            TransferAsset(
                "otherSelf@otherDomain",
                "${exchangeBillingTemplate}otherDomain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction5
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@otherDomain",
                "otherSelf@otherDomain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction6
            )
        )
        transferRepo.save(
            TransferAsset(
                "hisSelf@otherDomain",
                "${exchangeBillingTemplate}otherDomain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction6
            )
        )
    }


    private fun prepareDataBlock0() {
        val block0 = blockRepo.save(Block(0, 0))
        val batch0 = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )

        var transaction0 = txRepo.save(Transaction(null, block0, "mySelf@$domain", 1, false, batch = batch0))
        var transaction1 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false, batch = batch0))

        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction0
            )
        )
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction0
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction1
            )
        )
        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction1
            )
        )
    }

}
