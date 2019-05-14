/*
 *
 *  Copyright D3 Ledger, Inc. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 * /
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

    @Test
    fun testSelectByAccountIDAndAssetId() {
        val commulativeAmount = BigDecimal("24")
        val lastAssetSum = BigDecimal("908")
        val entity = repo.save(
            AccountAssetCustodyContext(
                accountId = "some_account@some_domain",
                assetId = "some_asset@some_domain",
                commulativeFeeAmount = commulativeAmount,
                lastTransferTimestamp = 1243L,
                lastAssetSum = lastAssetSum
            )
        )

        val found = repo.selectByAccountAndAssetId(entity.accountId, entity.assetId).get()

        assertEquals(commulativeAmount, found.commulativeFeeAmount)
        assertEquals(lastAssetSum, found.lastAssetSum)

    }
}
