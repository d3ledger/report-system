/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.model

import com.d3.report.dto.Conflict
import java.lang.RuntimeException
import java.math.BigDecimal

class InvalidValue(message: String) : RuntimeException(message)

class Transfer(
    var transfer: TransferAsset? = null,
    var fee: TransferAsset? = null
)

class AccountRegistration(
    val accountId: String? = null,
    val registrationTime: Long? = null
)

class AccountCustody(
    val accountId: String? = null,
    val assetCustody: HashMap<String, BigDecimal> = HashMap()
)

open class BaseReport(
    var total: Long,
    var pages: Int,
    override val errorCode: String?,
    override val errorMessage: String?
) : Conflict

class TransferReport(
    val transfers: ArrayList<Transfer> = ArrayList(),
    total: Long = 0,
    pages: Int = 0,
    code: String? = null,
    message: String? = null
) : BaseReport(total, pages, code, message)

class RegistrationReport(
    val accounts: List<AccountRegistration> = emptyList(),
    total: Long = 0,
    pages: Int = 0,
    code: String? = null,
    message: String? = null
) : BaseReport(total, pages, code, message)

class CustodyReport(
    val accounts: List<AccountCustody> = emptyList(),
    total: Long = 0,
    pages: Int = 0,
    code: String? = null,
    message: String? = null
) : BaseReport(total, pages, code, message)

class ExchangeReport(
    val batches: List<TransactionBatchEntity> = emptyList(),
    total: Long = 0,
    pages: Int = 0,
    code: String? = null,
    message: String? = null
) : BaseReport(total, pages, code, message)
