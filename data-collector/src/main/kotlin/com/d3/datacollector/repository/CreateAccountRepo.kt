/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.repository

import com.d3.datacollector.model.CreateAccount
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface CreateAccountRepo : CrudRepository<CreateAccount, Long?> {

    @Query("SELECT a FROM CreateAccount a WHERE CONCAT(a.accountName,'@',a.domainId) = :accountId ")
    fun findByAccountId(accountId:String): Optional<CreateAccount>
}
