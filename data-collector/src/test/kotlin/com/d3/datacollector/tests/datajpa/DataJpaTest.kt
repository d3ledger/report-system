package com.d3.datacollector.tests.datajpa

import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.CreateAccount
import com.d3.datacollector.model.State
import com.d3.datacollector.model.Transaction
import com.d3.datacollector.repository.BillingRepository
import com.d3.datacollector.repository.CreateAccountRepo
import com.d3.datacollector.repository.StateRepository
import com.d3.datacollector.repository.TransactionRepo
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


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

    @Test
    @Transactional
    fun testBillingRepository() {
        val feeFraction = "0.55"
        val billing = Billing(feeFraction = BigDecimal(feeFraction))
        em.persist(billing)
        em.flush()

        var found = billingRepo.findById(billing.id!!)

        assertTrue(found.isPresent)

        found = billingRepo.selectByAccountIdBillingTypeAndAsset(billing.accountId, billing.asset, billing.billingType)
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
}
