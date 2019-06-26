/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.service

import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.State
import com.d3.datacollector.repository.BillingRepository
import com.d3.datacollector.repository.StateRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class DbService {
    @Autowired
    lateinit var stateRepo: StateRepository
    @Autowired
    lateinit var billingRepo: BillingRepository

    @Transactional
    fun updateBillingInDb(
        billing: Billing
    ): Billing {
        val found =
            billingRepo.selectByAccountIdBillingTypeAndAsset(billing.accountId, billing.asset, billing.billingType)
        return if (found.isPresent) {
            val toUpdate = found.get()
            val updated = Billing(
                id = toUpdate.id,
                accountId = toUpdate.accountId,
                billingType = toUpdate.billingType,
                asset = toUpdate.asset,
                feeFraction = billing.feeFraction,
                created = toUpdate.created,
                updated = Date().time
            )
            billingRepo.save(updated)
        } else {
            billingRepo.save(billing)
        }
    }

    @Transactional
    fun updateStateInDb(
        lastBlockState: State,
        lastRequest: State
    ) {
        var newLastBlock = lastBlockState.value.toLong()
        newLastBlock++
        lastBlockState.value = newLastBlock.toString()
        stateRepo.save(lastBlockState)
        var newQueryNumber = lastRequest.value.toLong()
        newQueryNumber++
        lastRequest.value = newQueryNumber.toString()
        stateRepo.save(lastRequest)
    }
}
