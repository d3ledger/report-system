/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.tests.datajpa

import com.d3.report.model.AccountAssetCustodyContext
import com.d3.report.repository.AccountAssetCustodyContextRepo
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@DataJpaTest
class AccountAssetCustodyContextRepoTest {

    @Autowired
    private lateinit var repo: AccountAssetCustodyContextRepo

    /**
     * @given three contexts for same asset and account
     * @when get context from db by account, asset, time
     * @then returned context for current asset and account with max(lastTransferTime) before time send in request
     */
    @Test
    fun testSelectByAccountIDAndAssetId() {
        val commulativeAmount = BigDecimal("24")
        val lastAssetSum = BigDecimal("908")
        val lastTransferTime = 1243L
        repo.save(
            AccountAssetCustodyContext(
                accountId = "some_account@some_domain",
                assetId = "some_asset@some_domain",
                commulativeFeeAmount = BigDecimal("42"),
                lastTransferTimestamp = 221L,
                lastAssetSum = BigDecimal("234")
            )
        )

        val entity = repo.save(
            AccountAssetCustodyContext(
                accountId = "some_account@some_domain",
                assetId = "some_asset@some_domain",
                commulativeFeeAmount = commulativeAmount,
                lastTransferTimestamp = lastTransferTime,
                lastAssetSum = lastAssetSum
            )
        )

        repo.save(
            AccountAssetCustodyContext(
                accountId = "some_account@some_domain",
                assetId = "some_asset@some_domain",
                commulativeFeeAmount = BigDecimal("95"),
                lastTransferTimestamp = 27389L,
                lastAssetSum = BigDecimal("4223")
            )
        )

        val found = repo.selectByTimeAndAccountAndAssetId(entity.accountId, entity.assetId, lastTransferTime).get()

        assertEquals(commulativeAmount, found.commulativeFeeAmount)
        assertEquals(lastAssetSum, found.lastAssetSum)
        assertEquals(lastTransferTime, found.lastTransferTimestamp)
    }
}
