/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.dto

import com.d3.report.model.BaseReport
import com.d3.report.model.Block
import com.d3.report.model.TransactionBatchEntity
import com.d3.report.model.TransferAsset

interface Conflict {
    val errorCode: String?
    val errorMessage: String?
}

class ExchangeTransactionDto(
    val id: Long? = null,
    val block: Block? = null,
    val creatorId: String? = null,
    val quorum: Int? = null,
    var rejected: Boolean = false,
    val commands: MutableList<TransferAsset> = ArrayList()
)

class ExchangeTransactionBatchDto(
    val id: Long? = null,
    var transactions: List<ExchangeTransactionDto> = ArrayList(),
    val batchType: TransactionBatchEntity.BatchType = TransactionBatchEntity.BatchType.UNDEFINED
)

class ExchangeReportDto(
    val batches: List<ExchangeTransactionBatchDto> = emptyList(),
    total: Long = 0,
    pages: Int = 0,
    code: String? = null,
    message: String? = null
) : BaseReport(total, pages, code, message)
