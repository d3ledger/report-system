package com.d3.report.repository

import com.d3.report.model.TransferAsset
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransferAssetRepo : CrudRepository<TransferAsset, Long?> {

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false " +
            "and exists (SELECT ta FROM TransferAsset ta WHERE ta.transaction.id = t.transaction.id and ta.destAccountId LIKE CONCAT('%',:accTemplate,'%')) " +
            "and t.transaction.block.blockCreationTime Between :from and :to")
    fun getDataBetween(accTemplate:String, from: Long, to: Long, pageable: Pageable): Page<TransferAsset>

}
