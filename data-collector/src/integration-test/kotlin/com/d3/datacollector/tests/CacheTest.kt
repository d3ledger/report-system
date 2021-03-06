/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.tests

import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.Billing
import com.d3.datacollector.repository.BillingRepository
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import javax.transaction.Transactional
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@SpringBootTest
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
                domainName = "itisdomain",
                asset = "anyTestAsset#itisdomain"
            )
        )
        billingRepo.save(
            Billing(
                domainName = "otherdomain",
                asset = "otherAsset#otherdomain"
            )
        )

        billingRepo.save(
            Billing(
                domainName = "otherdomain",
                asset = "otherAsset#otherdomain",
                billingType = Billing.BillingTypeEnum.WITHDRAWAL
            )
        )

        cache.init()

        assertEquals(2, cache.getTransferFee().size)
        assertEquals(1, cache.getWithdrawalFee().size)
    }
}
