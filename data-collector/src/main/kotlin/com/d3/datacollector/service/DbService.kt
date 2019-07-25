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
    fun markBlockProcessed(lastBlockProcessed: Long) =
        saveNewBlockInfo(lastBlockProcessed, LAST_PROCESSED_BLOCK_ROW_ID)

    @Transactional
    fun markBlockSeen(blockNumber: Long) = saveNewBlockInfo(blockNumber, LAST_SEEN_BLOCK_ROW_ID)

    private fun saveNewBlockInfo(blockNumber: Long, rowId: Long) {
        val currentBlock = stateRepo.findById(rowId)
        if (currentBlock.isPresent) {
            if (blockNumber <= currentBlock.get().value.toLong()) {
                throw IllegalArgumentException("Blocks must be processed in ascending height manner")
            }
            val state = currentBlock.get()
            state.value = blockNumber.toString()
            stateRepo.save(state)
        } else {
            throw IllegalStateException("DB does not contain $rowId record for state")
        }
    }

    @Transactional
    fun getLastBlockSeen() = getBlock(LAST_SEEN_BLOCK_ROW_ID)

    @Transactional
    fun getLastBlockProcessed() = getBlock(LAST_PROCESSED_BLOCK_ROW_ID)

    private fun getBlock(rowId: Long): Long {
        val currentBlock = stateRepo.findById(rowId)
        return if (currentBlock.isPresent) currentBlock.get().value.toLong() else 0
    }

    companion object {
        const val LAST_PROCESSED_BLOCK_ROW_ID = 1L
        const val LAST_SEEN_BLOCK_ROW_ID = 0L
    }
}
