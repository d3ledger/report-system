package com.d3.report.tests

import com.d3.report.model.RegistrationReport
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

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TestCustodyReport {

    private val mapper = ObjectMapper()

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun testCustodyFeeReport() {
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
        var respBody = mapper.readValue(result.response.contentAsString, RegistrationReport::class.java)
    }
}
