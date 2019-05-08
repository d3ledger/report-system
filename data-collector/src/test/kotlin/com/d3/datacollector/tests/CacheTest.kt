package com.d3.datacollector.tests

import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.Billing
import com.d3.datacollector.repository.BillingRepository
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import javax.transaction.Transactional
import kotlin.test.assertEquals
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
@RunWith(SpringRunner::class)
@SpringBootTest
@TestPropertySource(properties = arrayOf("app.scheduling.enable=false", "app.rabbitmq.enable=false"))
class CacheTest {
    @Autowired
    lateinit var billingRepo: BillingRepository
    @Autowired
    lateinit var cache: CacheRepository

    @Test
    @Transactional
    @Ignore
    fun testInitCache() {
        assertEquals(0, cache.getTransferFee().size)

        billingRepo.save(
            Billing(
                accountId = "demper@itisdomain",
                asset = "anyTestAsset#itisdomain"
            )
        )
        billingRepo.save(
            Billing(
                accountId = "other@otherdomain",
                asset = "otherAsset#otherdomain"
            )
        )

        billingRepo.save(
            Billing(
                accountId = "other@otherdomain",
                asset = "otherAsset#otherdomain",
                billingType = Billing.BillingTypeEnum.WITHDRAWAL
            )
        )

        cache.init()

        assertEquals(2, cache.getTransferFee().size)
        assertEquals(1, cache.getWithdrawalFee().size)
    }
}
