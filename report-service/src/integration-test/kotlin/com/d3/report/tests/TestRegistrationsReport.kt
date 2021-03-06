/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.tests

import com.d3.report.model.*
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.repository.SetAccountDetailRepo
import com.d3.report.repository.TransactionRepo
import com.fasterxml.jackson.databind.ObjectMapper
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
class TestRegistrationsReport {

    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var transactionRepo: TransactionRepo
    @Autowired
    lateinit var accountDetailRepo: SetAccountDetailRepo
    @Autowired
    lateinit var accountRepo: CreateAccountRepo
    @Value("\${iroha.templates.clientsStorage}")
    private lateinit var clientsStorageTemplate: String

    private val mapper = ObjectMapper()
    private val domain = "test"

    private val domainTest = "test"
    private val domainNew = "domainNew"

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    @Transactional
    fun testNetworkRegistrationsReport() {
        prepareDataSystemTest()
        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/registeredAccounts/system")
                    .param("from", "9")
                    .param("to", "99")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, RegistrationReport::class.java)

        assertEquals(3, respBody.accounts.size)
        assertEquals(10,respBody.accounts[0].registrationTime)
    }

    @Test
    @Transactional
    fun testDomainRegistrationsReport() {
        prepareDataAgentTest()

        val result: MvcResult = mvc
            .perform(
                MockMvcRequestBuilders.get("/report/billing/registeredAccounts/domain")
                    .param("domain", domain)
                    .param("from", "9")
                    .param("to", "99")
                    .param("pageNum", "1")
                    .param("pageSize", "10")
            )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val respBody = mapper.readValue(result.response.contentAsString, RegistrationReport::class.java)

        assertEquals(1, respBody.accounts.size)
        assertEquals("title2@$domain",respBody.accounts[0].accountId)
        assertEquals(10,respBody.accounts[0].registrationTime)
    }

    private fun prepareDataSystemTest() {

        var block0 = Block(1, 8)
        block0 = blockRepo.save(block0)
        var transaction0 = Transaction(null, block0, "yourSelf@author", 1, false)
        transaction0 = transactionRepo.save(transaction0)
        val accountDetail0 = SetAccountDetail("$clientsStorageTemplate$domainTest", "title1@$domainTest", domainTest, transaction0)
        accountDetailRepo.save(accountDetail0)
        val accName = clientsStorageTemplate.replace("@","")
        val createAccount0 = CreateAccount(accName, domainTest,"Some public key",transaction0)
        accountRepo.save(createAccount0)
        val createAccount1 = CreateAccount(accName, "domainNew","Some public key",transaction0)
        accountRepo.save(createAccount1)

        var block1 = Block(2, 10)
        block1 = blockRepo.save(block1)

        var transaction1 = Transaction(null, block1, "mySelf@author", 1, false)
        transaction1 = transactionRepo.save(transaction1)

        val accountDetail1 = SetAccountDetail("$clientsStorageTemplate$domainTest", "title3@$domainTest", domainTest, transaction1)
        accountDetailRepo.save(accountDetail1)

        val accountDetail1a = SetAccountDetail("${clientsStorageTemplate}testOther", "title4@$domainTest", domainTest, transaction1)
        accountDetailRepo.save(accountDetail1a)

        val accountDetail2 = SetAccountDetail("$clientsStorageTemplate$domainTest", "title2@$domainTest", domainTest, transaction1)
        accountDetailRepo.save(accountDetail2)

        val accountDetail3 = SetAccountDetail("$clientsStorageTemplate$domainNew", "title2@$domainNew", domainNew, transaction1)
        accountDetailRepo.save(accountDetail3)
    }

    private fun prepareDataAgentTest() {

        var block0 = Block(1, 8)
        block0 = blockRepo.save(block0)
        var transaction0 = Transaction(null, block0, "yourSelf@author", 1, false)
        transaction0 = transactionRepo.save(transaction0)
        val accountDetail0 = SetAccountDetail("$clientsStorageTemplate$domain", "title1@$domain", domain, transaction0)
        accountDetailRepo.save(accountDetail0)

        var block1 = Block(2, 10)
        block1 = blockRepo.save(block1)
        var transaction1 = Transaction(null, block1, "mySelf@author", 1, false)
        transaction1 = transactionRepo.save(transaction1)
        val accountDetail1 = SetAccountDetail("$clientsStorageTemplate$domain", "title2@$domain", domain, transaction1)
        accountDetailRepo.save(accountDetail1)

        var block1a = Block(3, 11)
        block1a = blockRepo.save(block1a)
        var transaction1a = Transaction(null, block1a, "mySelf@author", 1, false)
        transaction1a = transactionRepo.save(transaction1a)
        val accountDetail1a = SetAccountDetail("${clientsStorageTemplate}testOther", "title3@$domain", domain, transaction1a)
        accountDetailRepo.save(accountDetail1a)

        var block2 = Block(4, 111)
        block2 = blockRepo.save(block2)
        var transaction2 = Transaction(null, block2, "yourSelf@author", 1, false)
        transaction2 = transactionRepo.save(transaction2)
        val accountDetail2 = SetAccountDetail("$clientsStorageTemplate$domain", "title4@$domain", domain, transaction2)
        accountDetailRepo.save(accountDetail2)
    }
}
