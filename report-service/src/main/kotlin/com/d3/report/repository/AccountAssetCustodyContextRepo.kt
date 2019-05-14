/*
 *
 *  Copyright D3 Ledger, Inc. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 * /
 */

package com.d3.report.repository

import com.d3.report.model.AccountAssetCustodyContext
import com.d3.report.model.Billing
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AccountAssetCustodyContextRepo : CrudRepository<AccountAssetCustodyContext, Long> {

    @Query("SELECT c FROM AccountAssetCustodyContext c WHERE c.accountId = :accountId and c.asset = :assetId")
    fun selectByAccountAndAssetId(
        accountId: String,
        assetId: String
    ): Optional<Billing>
}
