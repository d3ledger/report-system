/*
 *  Copyright D3 Ledger, Inc. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.repository

import com.d3.report.model.AccountAssetCustodyContext
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface AccountAssetCustodyContextRepo : CrudRepository<AccountAssetCustodyContext, Long> {

    @Query("SELECT c FROM AccountAssetCustodyContext c WHERE c.accountId = :accountId" +
            " and c.assetId = :assetId and c.lastTransferTimestamp =" +
            " (SELECT MAX(c.lastTransferTimestamp) FROM AccountAssetCustodyContext c WHERE c.accountId = :accountId AND c.assetId = :assetId and c.lastTransferTimestamp <= :nearTime)")
    fun selectByTimeAndAccountAndAssetId(
        accountId: String,
        assetId: String,
        nearTime: Long
    ): Optional<AccountAssetCustodyContext>
}
