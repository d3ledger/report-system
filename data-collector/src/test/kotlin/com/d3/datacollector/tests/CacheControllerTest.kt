/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.tests

import com.d3.datacollector.engine.TestEnv
import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.BillingResponse
import com.d3.datacollector.model.SingleBillingResponse
import com.d3.datacollector.utils.getDomainFromAccountId
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import javax.transaction.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = ["app.rabbitmq.enable=false"])
class CacheControllerTest : TestEnv() {

    /**
     * TODO Update test. Add all type of fees testing
     */
    @Test
    @Transactional
    fun testGetBillling() {
        val bittingGlobbaly = "bitting@globbaly"
        val someAsset = "someAsset"
        val fee = "0.5"

        cache.addFeeByType(
            Billing(
                accountId = bittingGlobbaly,
                asset = someAsset,
                feeFraction = BigDecimal(fee)
            )
        )

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, BillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        val domain = getDomainFromAccountId(bittingGlobbaly)
        assertEquals(
            BigDecimal(fee),
            respBody.transfer[domain]!![someAsset]!!.feeFraction
        )
    }

    @Test
    @Transactional
    fun testGetSingleBillling() {
        val domain = "globbaly"
        val bittingGlobbaly = "bitting@$domain"
        val someAsset = "someAsset"
        val fee = "0.5"

        cache.addFeeByType(
            Billing(
                accountId = bittingGlobbaly,
                asset = "$someAsset#$domain",
                feeFraction = BigDecimal(fee)
            )
        )

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing/$domain/$someAsset/TRANSFER"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, SingleBillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertEquals(
            BigDecimal(fee),
            respBody.billing.feeFraction
        )
    }
}
