package com.d3.report.repository

import com.d3.report.model.TransferAsset
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransferAssetRepo : CrudRepository<TransferAsset, Long?> {

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false and t.transaction.block.blockCreationTime Between :from and :to")
    fun getDataBetween(from: Long, to: Long): List<TransferAsset>
}
