/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.repository

import com.d3.report.model.CreateAccount
import com.d3.report.model.TransferAsset
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface CreateAccountRepo : CrudRepository<CreateAccount, Long?> {


    @Query("SELECT ca FROM CreateAccount ca WHERE ca.accountName = :accName")
    fun findAccountsByName(accName:String):List<CreateAccount>

    @Query("SELECT ca FROM CreateAccount ca WHERE ca.transaction.rejected = false " +
            "and ca.domainId = :domain")
    fun getDomainAccounts(domain:String, pageable: Pageable): Page<CreateAccount>

    @Query("SELECT ca FROM CreateAccount ca WHERE ca.transaction.rejected = false ")
    fun getAllAccounts(pageable: Pageable): Page<CreateAccount>
}
