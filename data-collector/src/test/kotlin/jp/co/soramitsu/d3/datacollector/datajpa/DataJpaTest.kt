package jp.co.soramitsu.d3.datacollector.datajpa

import jp.co.soramitsu.d3.datacollector.model.Billing
import jp.co.soramitsu.d3.datacollector.model.State
import jp.co.soramitsu.d3.datacollector.repository.BillingRepository
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@RunWith(SpringRunner::class)
@DataJpaTest
@TestPropertySource(properties = arrayOf(
    "spring.jpa.hibernate.ddl-auto=validate"))
class DataJpaTest {

    @Autowired
    lateinit var em: TestEntityManager

    @Autowired
    lateinit var stateRepo: StateRepository

    @Autowired
    lateinit var billingRepo: BillingRepository

    @Test
    fun testBillingRepository() {
        val feeFraction = "0.55"
        val billing = Billing(feeFraction = BigDecimal(feeFraction))
        em.persist(billing)
        em.flush()

        var found = billingRepo.findById(billing.id!!)

        assertTrue(found.isPresent)

        found = billingRepo.selectByAccountIdBillingTypeAndAsset(billing.accountId, billing.asset,billing.billingType)
        assertTrue(found.isPresent)
        assertEquals(billing.id, found.get().id)

        val updated = Billing(id = found.get().id, feeFraction = BigDecimal("0.15"),created = billing.created)

        billingRepo.save(updated)
        em.flush()

        found = billingRepo.findById(billing.id!!)
        assertEquals(updated.feeFraction, found.get().feeFraction)
        assertEquals(billing.id, found.get().id)
    }

    @Test
    fun testStateRepository() {
        // given
        val state1 = State(null, "sdfs","sdfsf")
        em.persist(state1)
        em.flush()

        // when
        val found = stateRepo.findById(state1.id!!)

        // then
        assertThat(found.get().value)
            .isEqualTo(state1.value)
    }

}
