/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.service

import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.Block
import com.d3.datacollector.repository.BillingRepository
import com.d3.datacollector.repository.BlockRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
class DbService {
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var billingRepo: BillingRepository

    @Transactional
    fun updateBillingInDb(
        billing: Billing
    ): Billing {
        val found =
            billingRepo.selectExistingBillingInfo(
                billing.domainName,
                billing.asset,
                billing.billingType,
                billing.destination,
                billing.feeDescription,
                billing.minAmount,
                billing.maxAmount
            )
        return if (found.isPresent) {
            val toUpdate = found.get()
            val updated = Billing(
                id = toUpdate.id,
                feeDescription = toUpdate.feeDescription,
                domainName = toUpdate.domainName,
                billingType = toUpdate.billingType,
                asset = toUpdate.asset,
                destination = toUpdate.destination,
                feeType = billing.feeType,
                feeNature = billing.feeNature,
                feeComputation = billing.feeComputation,
                feeAccount = billing.feeAccount,
                feeFraction = billing.feeFraction,
                minAmount = billing.minAmount,
                maxAmount = billing.maxAmount,
                minFee = billing.minFee,
                maxFee = billing.maxFee,
                created = toUpdate.created
            )
            billingRepo.save(updated)
        } else {
            billingRepo.save(billing)
        }
    }

    @Transactional
    fun saveNewBlock(block: Block): Block {
        val currentBlockHeight = getLastBlockProcessedHeight()
        if (block.blockNumber!! - currentBlockHeight != 1L) {
            throw IllegalArgumentException("Blocks must be processed sequentially " +
                    "(current ${block.blockNumber}, last processed $currentBlockHeight})")
        }
        return blockRepo.save(block)
    }

    fun getLastBlockProcessedHeight() = blockRepo.count()
}
