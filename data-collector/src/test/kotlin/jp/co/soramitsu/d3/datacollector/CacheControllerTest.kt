package jp.co.soramitsu.d3.datacollector

import com.fasterxml.jackson.databind.ObjectMapper
import jp.co.soramitsu.d3.datacollector.cache.CacheRepository
import jp.co.soramitsu.d3.datacollector.model.BillingResponse
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CacheControllerTest {

    private val mapper = ObjectMapper()

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var cache: CacheRepository

    @Test
    fun testGetBillling() {
        val bittingGlobbaly = "bitting@globbaly"
        val someAsset = "someAsset"
        val fee = "0.5"

        cache.addTransferBilling(bittingGlobbaly, someAsset, BigDecimal(fee))

        var result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        var respBody = mapper.readValue(result.response.contentAsString, BillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(BigDecimal(fee),respBody.transferBilling.get(bittingGlobbaly)!!.get(someAsset))
    }
}
