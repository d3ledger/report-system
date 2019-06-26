package com.d3.datacollector.tests.reports

import com.d3.datacollector.engine.TestEnv
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import mu.KLogging
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.net.URI

/**
 * This class contains snippets for generating DB data from Iroha for Reports testing.
 */
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = arrayOf("app.scheduling.enable=false", "app.rabbitmq.enable=false"))
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PrepeareCustodyReportTest : TestEnv() {


private val log = KLogging().logger

    @Test
    @Ignore
    fun PrepeareDataForCustodyReport() {
        val iroha = IrohaContainer()
            .withPeerConfig(peerConfig)



        iroha.start()
        blockTaskService.irohaService.toriiAddress = iroha.toriiAddress.toString()
        val api = IrohaAPI(URI(iroha.toriiAddress.toString()))

        iroha.stop()
    }
}
