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
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.PropertySources
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
class TestReportWeb {

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

    @Test
    @Transactional
    fun testTransferReport() {
        prepareData()
        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/transferAsset")
                    .param("from", "1")
                    .param("to", "99999")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, TransferReport::class.java)

        assertEquals(1, respBody.transfers.size)
        assertNotNull(respBody.transfers[0].fee)
    }

    private fun prepareData() {
        var block1 = Block(1, 129)
        block1 = blockRepo.save(block1)
        var transaction1 = Transaction(null, block1, "mySelf@author", 1, false)
        transaction1 = transactionRepo.save(transaction1)
        val transfer1 =
            TransferAsset("srcAcc@author", "destAcc@author", "assetId@author", null, BigDecimal("10"), transaction1)
        transferRepo.save(transfer1)

        var block2 = Block(2, 1299)
        block2 = blockRepo.save(block2)
        var transaction2 = Transaction(null, block2, "mySelf@author", 1, false)
        transaction2 = transactionRepo.save(transaction2)
        val transfer2 =
            TransferAsset("srcAcc@author", "destAcc@author", "assetId@author", null, BigDecimal("20"), transaction2)
        transferRepo.save(transfer2)

        var block3 = Block(3, 1398)
        block3 = blockRepo.save(block3)
        var transaction3 = Transaction(null, block3, "mySelf@author", 1, false)
        transaction3 = transactionRepo.save(transaction3)
        val transfer3 =
            TransferAsset("srcAcc@author", "destAcc@author", "assetId@author", null, BigDecimal("20"), transaction3)
        transferRepo.save(transfer3)

        var block4 = Block(4, 1499)
        block4 = blockRepo.save(block4)
        var transaction4 = Transaction(null, block4, "mySelf@author", 1, true)
        transaction4 = transactionRepo.save(transaction4)
        val transfer4 =
            TransferAsset("srcAcc@author", "destAcc@author", "assetId@author", null, BigDecimal("20"), transaction4)
        transferRepo.save(transfer4)

        val transfer5 =
            TransferAsset("srcAcc@author", "${transferBillingTemplate}author", "assetId@author", null, BigDecimal("0.2"), transaction3)
        transferRepo.save(transfer5)
    }
}
