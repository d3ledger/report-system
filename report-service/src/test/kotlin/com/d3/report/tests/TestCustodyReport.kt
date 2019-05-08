package com.d3.report.tests

import com.d3.report.model.Block
import com.d3.report.model.CreateAccount
import com.d3.report.model.CustodyReport
import com.d3.report.model.Transaction
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.repository.TransactionRepo
import com.d3.report.repository.TransferAssetRepo
import com.fasterxml.jackson.databind.ObjectMapper
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
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TestCustodyReport {

    private val mapper = ObjectMapper()

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

    val testDomain = "test_domain"

    @Test
    fun testCustodyFeeReportEmpty() {
        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/custody/agent")
                    .param("domain", "test_domain")
                    .param("to", "99")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, CustodyReport::class.java)
        assertEquals(0, respBody.accounts.size)
    }

    /**
     * TODO test is not finished, just started
     */
    @Test
    fun testCustodyFeeReport() {
        prepeareData()

        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/custody/agent")
                    .param("domain", testDomain)
                    .param("to", "99")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, CustodyReport::class.java)
        assertEquals(1, respBody.accounts.size)
    }

    private fun prepeareData() {
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
        val account1 = accountRepo.save(
            CreateAccount(
                "account1",
                testDomain,
                "publicKey1",
                transaction1
            )
        )
    }
}
