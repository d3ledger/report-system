/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.repository

import com.d3.datacollector.model.Billing
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal
import java.util.*

interface BillingRepository : CrudRepository<Billing, Long> {

    @Query("SELECT b FROM Billing b " +
            "WHERE b.domainName = :domainName and b.asset = :asset and b.billingType = :billingType" +
            " and b.destination = :destination and b.feeDescription= :feeDescription" +
            " and b.minAmount = :minAmount and b.maxAmount = :maxAmount")
    fun selectExistingBillingInfo(
        domainName: String,
        asset: String,
        billingType: Billing.BillingTypeEnum,
        destination: String,
        feeDescription: String,
        minAmount: BigDecimal,
        maxAmount: BigDecimal
    ): Optional<Billing>
}
