package com.d3.report.repository
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
import com.d3.report.model.Billing
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface BillingRepository : CrudRepository<Billing, Long> {

    @Query("SELECT b FROM Billing b WHERE b.accountId = ?1 and b.asset = ?2 and b.billingType = ?3")
    fun selectByAccountIdBillingTypeAndAsset(
        accountId: String,
        asset: String,
        billingType: Billing.BillingTypeEnum
    ): Optional<Billing>
}
