package com.d3.report.tests.withexternalgeneration

import com.d3.report.model.CustodyReport
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Ignore
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
import javax.transaction.Transactional
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TestCustodyReportWithIroha {

    private val mapper = ObjectMapper()

    @Value("\${iroha.templates.custodyBilling}")
    private var custodyBillingTemplate = "custody_billing@"

    @Value("\${billing.custody.period}")
    private var custodyPeriod: Long = 86400000

    @Autowired
    lateinit var mvc: MockMvc

    /**
     * @given it depends
     * @when custody report calculated for two days
     * @then fee should be equal fee of two days
     */
    @Test
    @Ignore
    fun testCustodyFeeReportDataCustomer() {

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/custody/customer")
                    .param("accountId", "user_b@bank")
                    .param("to", "1659556640625")
                    .param("from", "0")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, CustodyReport::class.java)
        assertEquals(1, respBody.accounts.size)
        assertEquals("user_b@bank", respBody.accounts[0].accountId)
        assertEquals(1, respBody.accounts[0].assetCustody.size)
    }

}
