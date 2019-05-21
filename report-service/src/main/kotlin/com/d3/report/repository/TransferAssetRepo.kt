/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.repository

import com.d3.report.model.TransferAsset
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface TransferAssetRepo : CrudRepository<TransferAsset, Long?> {

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false " +
            "and exists (SELECT ta FROM TransferAsset ta WHERE ta.transaction.id = t.transaction.id and ta.destAccountId = :billingAccountId) " +
            "and t.transaction.block.blockCreationTime Between :from and :to")
    fun getDataBetweenForBillingAccount(billingAccountId:String, from: Long, to: Long, pageable: Pageable): Page<TransferAsset>

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false " +
            "and (t.srcAccountId = :account OR  t.destAccountId = :account) " +
            "ORDER BY t.transaction.block.blockCreationTime ASC")
    fun getAllTransfersForAccountInAndOut(account:String, pageable: Pageable): Page<TransferAsset>

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false " +
            "and (t.srcAccountId = :account OR  t.destAccountId = :account)" +
            " and t.transaction.block.blockCreationTime < :to" +
            " ORDER BY t.transaction.block.blockCreationTime ASC")
    fun getAllTransfersForAccountInAndOutTillTo(account:String, to:Long, pageable: Pageable): Page<TransferAsset>

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false " +
            "and exists (SELECT ta FROM TransferAsset ta WHERE ta.transaction.id = t.transaction.id and ta.destAccountId LIKE CONCAT(:billingAccountTemplate,'%')) " +
            "and t.transaction.block.blockCreationTime Between :from and :to")
    fun getDataBetweenForBillingAccountTemplate(billingAccountTemplate:String, from: Long, to: Long, pageable: Pageable): Page<TransferAsset>

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false " +
            "and (t.srcAccountId = :account OR  t.destAccountId = :account)" +
            " and t.transaction.block.blockCreationTime < :to" +
            " ORDER BY t.transaction.block.blockCreationTime ASC")
    fun getTimedBilledTransfersForAccount(account:String, to:Long, pageable: Pageable): Page<TransferAsset>

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false" +
            " and exists (SELECT ta FROM TransferAsset ta WHERE ta.transaction.id = t.transaction.id and ta.destAccountId = :billingAccount)" +
            " and t.srcAccountId = :accountId" +
            " and t.transaction.block.blockCreationTime Between :from and :to")
    fun getDataBetweenForBillingAccount(accountId:String, billingAccount:String, from: Long, to: Long, pageable: Pageable): Page<TransferAsset>

    @Query("SELECT t FROM TransferAsset t WHERE t.transaction.rejected = false" +
            " and exists (SELECT ta FROM TransferAsset ta WHERE ta.transaction.id = t.transaction.id and ta.destAccountId = :billingAccount)" +
            " and t.srcAccountId = :accountId" +
            " and t.assetId = :assetId" +
            " and t.transaction.block.blockCreationTime Between :from and :to")
    fun getDataBetweenForAssetOfAccount(assetId:String, accountId:String, billingAccount:String, from: Long, to: Long, pageable: Pageable): Page<TransferAsset>

}
