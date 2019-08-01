/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.tests.datajpa

import com.d3.datacollector.model.*
import com.d3.datacollector.repository.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import javax.transaction.Transactional
import kotlin.test.*

@RunWith(SpringRunner::class)
@DataJpaTest
@TestPropertySource(properties = ["spring.jpa.hibernate.ddl-auto=validate"])
class DataJpaTest {

    @Autowired
    lateinit var em: TestEntityManager

    @Autowired
    lateinit var stateRepo: StateRepository

    @Autowired
    lateinit var billingRepo: BillingRepository

    @Autowired
    lateinit var accountRepo: CreateAccountRepo

    @Autowired
    lateinit var transactionRepo: TransactionRepo

    @Autowired
    lateinit var quorumRepo: SetAccountQuorumRepo

    @Autowired
    lateinit var blockRepo:BlockRepository

    @Autowired
    lateinit var transferRepo:TransferAssetRepo

    /**
     * Test Find Account by accountId
     * @given Account saved in DB and accountId
     * @when execute query which checks that account with particular accountId
     * @then 1. Three cases exists
     * 2. not exists when name is different
     * 3. not exists when domain is different
     */
    @Test
    @Transactional
    fun testFindAccountByAccountId() {
        val name = "best"
        val domain = "iroha"
        val transaction = transactionRepo.save((Transaction()))
        accountRepo.save(CreateAccount(name, domain, "some public key", transaction))
        assertTrue(accountRepo.findByAccountId("$name@$domain").isPresent)
        assertFalse(accountRepo.findByAccountId("otherName@$domain").isPresent)
        assertFalse(accountRepo.findByAccountId("$name@otherDomain").isPresent)
    }

    /**
     * Test Find Account by accountId
     * @given Account saved in DB
     * @when Quorum record created for account
     * @then It is possible to find Quorum by accountId
     */
    @Test
    @Transactional
    fun testFindQuorumByAccountId() {
        val name = "best"
        val domain = "iroha"
        var block = Block(1, 12)
        block = blockRepo.save(block)
        val transaction = transactionRepo.save((Transaction(block = block)))
        accountRepo.save(CreateAccount(name, domain, "some public key", transaction))
        assertTrue(accountRepo.findByAccountId("$name@$domain").isPresent)
        val quorum = quorumRepo.save(SetAccountQuorum("$name@$domain", 2, transaction))
        assertTrue(quorumRepo.existsById(quorum.id!!))
        assertTrue(quorumRepo.getQuorumByAccountId(quorum.accountId!!).isNotEmpty())
    }

    /**
     * Test Find Account by accountId
     * @given Billing saved in DB
     * @when Billing found in DB
     * @then Iit is possible to find billing by accountId, asset and type
     */
    @Test
    @Transactional
    fun testBillingRepository() {
        val feeFraction = "0.55"
        val billing = Billing(feeFraction = BigDecimal(feeFraction))
        em.persist(billing)
        em.flush()

        var found = billingRepo.findById(billing.id!!)

        assertTrue(found.isPresent)

        found = billingRepo.selectByDomainBillingTypeAndAsset(billing.domainName, billing.asset, billing.billingType)
        assertTrue(found.isPresent)
        assertEquals(billing.id, found.get().id)

        val updated = Billing(
            id = found.get().id,
            feeFraction = BigDecimal("0.15"),
            created = billing.created
        )

        billingRepo.save(updated)
        em.flush()

        found = billingRepo.findById(billing.id!!)
        assertEquals(updated.feeFraction, found.get().feeFraction)
        assertEquals(billing.id, found.get().id)
        assertNotNull(billing.created)
        assertNotNull(found.get().updated)
    }

    /**
     * Test Find Account by accountId
     * @given State saved in DB
     * @when and found
     * @then value of saved is the same as of original
     */
    @Test
    @Transactional
    fun testStateRepository() {
        // given
        val state1 = State(null, "sdfs", "sdfsf")
        em.persist(state1)
        em.flush()

        // when
        val found = stateRepo.findById(state1.id!!)

        // then
        assertThat(found.get().value)
            .isEqualTo(state1.value)
    }

    /**
     * Test Very big transfer amount value
     * @given nothing
     * @when Transfer asset with very big amount sae
     * @then value of saved is the same as of original
     */
    @Test
    @Transactional
    fun testBiggestAmounts() {
        try {
            val block = blockRepo.save(Block(1, 11))
            val transaction = transactionRepo.save((Transaction(block = block)))
            val transfer = transferRepo.save(
                TransferAsset(
                    "srcAccountId",
                    "destAccountId",
                    "assetId",
                    "any decription",
                    BigDecimal("1618033988749894848204586834"),
                    transaction
                )
            )
            assertTrue(transfer.id != null)
        } catch (e:Exception) {
            fail()
        }
    }
}
