/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.tests

import com.d3.datacollector.controllers.SetRateDTO
import com.d3.datacollector.engine.TestEnv
import com.d3.datacollector.model.*
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

    // TODO Update test. Add all type of fees testing
    @Test
    @Transactional
    fun testGetBillling() {
        val anyDestination = "*"
        val domainName = "global"
        val feeAccountId = "account@$domainName"
        val assetId = "asset#$domainName"
        val feeFraction = BigDecimal("1000000000.12345678")
        val feeDescription = "FEE-1"

        val billing = Billing(
            feeDescription = feeDescription,
            domainName = domainName,
            asset = assetId,
            destination = anyDestination,
            feeType = Billing.FeeTypeEnum.FIXED,
            feeNature = Billing.FeeNatureEnum.TRANSFER,
            feeAccount = feeAccountId,
            feeFraction = feeFraction
        )
        cache.addBillingByType(billing)

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, BillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertTrue(respBody.transfer[domainName]!![assetId]!!.contains(billing.toDefaultBilling()))
    }

    @Test
    @Transactional
    fun testGetSingleBillling() {
        val domainName = "global"
        val assetName = "asset"
        val feeFraction = BigDecimal("1000000000.12345678")
        val precision = 0

        val billing = Billing(
            domainName = domainName,
            billingType = Billing.BillingTypeEnum.TRANSFER,
            asset = "$assetName#$domainName",
            feeFraction = feeFraction
        )
        val transaction = Transaction()
        transactionRepo.save(transaction)
        createAssetRepo.save(
            CreateAsset(
                assetName,
                domainName,
                precision,
                transaction
            )
        )
        cache.addBillingByType(billing)

        val result: MvcResult = mvc
            .perform(MockMvcRequestBuilders.get("/cache/get/billing/$domainName/$assetName/$domainName/TRANSFER"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, SingleBillingResponse::class.java)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)
        assertTrue(respBody.feeInfo.contains(billing.toDefaultBilling()))
        assertEquals(precision, respBody.assetPrecision)
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
        assertTrue(BigDecimal(getResultString) > BigDecimal.ZERO)
    }
}

fun Billing.toDefaultBilling() =
    Billing(
        feeDescription = feeDescription,
        destination = destination,
        feeType = feeType,
        feeNature = feeNature,
        feeFraction = feeFraction,
        feeComputation = feeComputation,
        feeAccount = feeAccount,
        minAmount = minAmount,
        maxAmount = maxAmount,
        minFee = minFee,
        maxFee = maxFee,
        created = created,
        updated = updated
    )
