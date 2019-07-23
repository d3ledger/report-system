/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.service

import com.d3.datacollector.model.Billing
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
            billingRepo.selectByAccountIdBillingTypeAndAsset(
                billing.accountId,
                billing.asset,
                billing.billingType
            )
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
    fun markBlockProcessed(
        lastBlockProcessed: Long
    ) {
        val currentBlock = stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID)
        if (currentBlock.isPresent && lastBlockProcessed - currentBlock.get().value.toLong() != 1L) {
            throw IllegalArgumentException("Blocks must be processed sequentially")
        }
        val block = currentBlock.get()
        block.value = lastBlockProcessed.toString()
        stateRepo.save(block)
    }

    @Transactional
    fun getLastBlockProcessed(): Long {
        val currentBlock = stateRepo.findById(LAST_PROCESSED_BLOCK_ROW_ID)
        return if (currentBlock.isPresent) currentBlock.get().value.toLong() else 0
    }

    companion object {
        const val LAST_PROCESSED_BLOCK_ROW_ID = 0L
    }
}
