package com.d3.report.repository

import com.d3.report.model.TransactionBatchEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransactionBatchRepo : CrudRepository<TransactionBatchEntity, Long> {

    @Query(
        "SELECT tb FROM TransactionBatchEntity tb WHERE" +
                " exists (SELECT tx FROM Transaction tx WHERE" +
                " tx.batch.id = tb.id" +
                " and tx.rejected = false" +
                " and exists (SELECT ta FROM TransferAsset ta WHERE ta.transaction.id = tx.id and ta.destAccountId = :billingAccountId)" +
                " and tx.block.blockCreationTime Between :from and :to)"
    )
    fun getDataBetweenForBillingAccount(
        billingAccountId: String,
        from: Long,
        to: Long,
        pageable: Pageable
    ): Page<TransactionBatchEntity>
}
