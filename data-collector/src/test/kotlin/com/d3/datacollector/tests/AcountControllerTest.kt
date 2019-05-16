/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.tests

import com.d3.datacollector.model.*
import com.d3.datacollector.repository.BlockRepository
import com.d3.datacollector.repository.CreateAccountRepo
import com.d3.datacollector.repository.SetAccountQuorumRepo
import com.d3.datacollector.repository.TransactionRepo
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.transaction.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = arrayOf("app.scheduling.enable=false", "app.rabbitmq.enable=false"))
class AcountControllerTest {

    private val mapper = ObjectMapper()

    @Autowired
    lateinit var accountRepo: CreateAccountRepo

    @Autowired
    lateinit var transactionRepo: TransactionRepo

    @Autowired
    lateinit var quorumRepo: SetAccountQuorumRepo

    @Autowired
    lateinit var blockRepo:BlockRepository

    @Autowired
    lateinit var mvc: MockMvc

    val name = "best"
    val domain = "iroha"

    @Test
    @Transactional
    fun testFindAccountQuorumNoAccount() {
        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/iroha/account/quorum")
                    .param("accountId", "$name@$domain")
            )
            .andExpect(MockMvcResultMatchers.status().`is`(409))
            .andReturn()

        val respBody = mapper.readValue(result.response.contentAsString, IntegerWrapper::class.java)
        assertEquals("BAD_REQUEST", respBody.errorCode)
    }

    @Test
    @Transactional
    fun testFindAccountQuorum() {
        val block0 = blockRepo.save(Block(1, 11))
        val transaction0 = transactionRepo.save((Transaction(block = block0)))
        accountRepo.save(CreateAccount(name, domain, "some public key", transaction0))
        assertTrue(accountRepo.findByAccountId("$name@$domain").isPresent)
        val quorum0 = quorumRepo.save(SetAccountQuorum("$name@$domain", 1, transaction0))
        assertTrue(quorumRepo.getQuorumByAccountId(quorum0.accountId!!).isNotEmpty())

        val block = blockRepo.save(Block(2, 12))
        val transaction = transactionRepo.save((Transaction(block = block)))
        val quorum = quorumRepo.save(SetAccountQuorum("$name@$domain", 2, transaction))
        assertTrue(quorumRepo.getQuorumByAccountId(quorum.accountId!!).isNotEmpty())

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/iroha/account/quorum")
                    .param("accountId", "$name@$domain")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val respBody = mapper.readValue(result.response.contentAsString, IntegerWrapper::class.java)
        assertEquals(2, respBody.itIs)
    }

    @Test
    @Transactional
    fun testFindAccountByAccountId() {
        val transaction = transactionRepo.save((Transaction()))
        accountRepo.save(CreateAccount(name, domain, "some public key", transaction))
        assertTrue(accountRepo.findByAccountId("$name@$domain").isPresent)
        assertFalse(accountRepo.findByAccountId("otherName@$domain").isPresent)
        assertFalse(accountRepo.findByAccountId("$name@otherDomain").isPresent)

        var result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/iroha/account/exists")
                    .param("accountId", "$name@$domain")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        var respBody = mapper.readValue(result.response.contentAsString, BooleanWrapper::class.java)
        assertTrue(respBody.itIs)
        assertNull(respBody.errorCode)
        assertNull(respBody.message)

        result = mvc
            .perform(
                MockMvcRequestBuilders.get("/iroha/account/exists")
                    .param("accountId", "otherName@$domain")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        respBody = mapper.readValue(result.response.contentAsString, BooleanWrapper::class.java)
        assertFalse(respBody.itIs)

        result = mvc
            .perform(
                MockMvcRequestBuilders.get("/iroha/account/exists")
                    .param("accountId", "$name@otherDomain")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        respBody = mapper.readValue(result.response.contentAsString, BooleanWrapper::class.java)
        assertFalse(respBody.itIs)
    }
}
