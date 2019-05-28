/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.repository

import com.d3.report.model.Transaction
import com.d3.report.model.TransactionBatchEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransactionRepo  : CrudRepository<Transaction, Long?> {

    @Query("SELECT t FROM Transaction t WHERE t.batch.id = :batchId")
    fun getTransactionsByBatchId(
        batchId: Long
    ): List<Transaction>
}
