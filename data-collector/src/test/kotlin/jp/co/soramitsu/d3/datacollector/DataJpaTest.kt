package jp.co.soramitsu.d3.datacollector

import jp.co.soramitsu.d3.datacollector.model.State
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@DataJpaTest
class DataJpaTest {

    @Autowired
    private val entityManager: TestEntityManager? = null

    @Autowired
    private val stateRepo: StateRepository? = null

    // write test cases here

    @Test
    fun whenFindByName_thenReturnEmployee() {
        // given
        val alex = State(null, "sdfs","sdfsf")
        entityManager?.persist(alex)
        entityManager?.flush()

        // when
        val found = stateRepo?.findById(alex.id)

        // then
        assertThat(found?.get()?.value)
            .isEqualTo(alex.value)
    }

}