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
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@RunWith(SpringRunner::class)
@DataJpaTest
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

        val found = billingRepo.findById(billing.id)

        assertTrue(found.isPresent)

    }

    @Test
    fun testStateRepository() {
        // given
        val state1 = State(null, "sdfs","sdfsf")
        em.persist(state1)
        em.flush()

        // when
        val found = stateRepo.findById(state1.id)

        // then
        assertThat(found.get().value)
            .isEqualTo(state1.value)
    }

}