/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.tests

import com.d3.datacollector.controllers.SetRateDTO
import com.d3.datacollector.engine.TestEnv
import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.BillingResponse
import com.d3.datacollector.model.SingleBillingResponse
import com.google.gson.JsonParser
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import javax.transaction.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ControllersTest : TestEnv() {

    /**
     * TODO Update test. Add all type of fees testing
     */
    @Test
    @Transactional
    fun testGetBillling() {
        val globbaly = "globbaly"
        val someAsset = "someAsset"
        val fee = "0.5"

        cache.addFeeByType(
            Billing(
                domainName = globbaly,
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
        assertEquals(
            BigDecimal(fee),
            respBody.transfer[globbaly]!![someAsset]!!.feeFraction
        )
    }

    @Test
    @Transactional
    fun testGetSingleBillling() {
        val domain = "globbaly"
        val someAsset = "someAsset"
        val fee = "0.5"

        cache.addFeeByType(
            Billing(
                domainName = domain,
                asset = "$someAsset#$domain",
                feeFraction = BigDecimal(fee)
            )
        )

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing/$domain/$someAsset/$domain/TRANSFER"))
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

    @Test
    @Transactional
    fun testSetSingleExchangeRate() {
        val postResult: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.post("/rates").contentType(MediaType.APPLICATION_JSON_UTF8).content(
                    mapper.writeValueAsString(SetRateDTO(usdName, bankDomain, "50", "passphrase"))
                )
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val resultString =
            JsonParser().parse(postResult.response.contentAsString).asJsonObject.get("itIs").asString
        assertEquals("50", resultString)

        val getResult: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/rates/$usdName/$bankDomain"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val getResultString =
            JsonParser().parse(getResult.response.contentAsString).asJsonObject.get("itIs").asString
        assertEquals("50", getResultString)
    }

    @Test
    @Transactional
    fun testGetEthGasPrice() {
        val getResult: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/gas"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val getResultString =
            JsonParser().parse(getResult.response.contentAsString).asJsonObject.get("itIs").asString
        assertTrue(BigDecimal(getResultString) > BigDecimal(100))
    }
}
