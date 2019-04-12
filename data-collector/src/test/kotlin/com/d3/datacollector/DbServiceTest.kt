package com.d3.datacollector

import com.d3.datacollector.model.Billing
import com.d3.datacollector.repository.BillingRepository
import com.d3.datacollector.service.DbService
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = arrayOf("app.scheduling.enable=false", "app.rabbitmq.enable=false"))
class DbServiceTest {

    @Autowired
    lateinit var dbService: DbService
    @Autowired
    lateinit var billingRepo: BillingRepository

    @Test
    fun testSaveUpdateBillingData() {
        val billing = Billing()
        dbService.updateBillingInDb(billing)
        assertTrue(billingRepo.findById(billing.id!!).isPresent)
        val updated = Billing(feeFraction = BigDecimal("0.12"))
        dbService.updateBillingInDb(updated)
        assertEquals(1, billingRepo.count())
        assertEquals(updated.feeFraction, billingRepo.findById(billing.id!!).get().feeFraction.stripTrailingZeros())
    }
}
