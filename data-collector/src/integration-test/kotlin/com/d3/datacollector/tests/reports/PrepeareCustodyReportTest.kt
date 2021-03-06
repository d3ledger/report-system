package com.d3.datacollector.tests.reports

import com.d3.datacollector.engine.TestEnv
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner

/**
 * This class contains snippets for generating DB data from Iroha for Reports testing.
 */
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PrepeareCustodyReportTest : TestEnv() {

    @Test
    @Ignore
    fun prepareDataForCustodyReport() {
        val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)

        iroha.start()
        iroha.stop()
    }
}
