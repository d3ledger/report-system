/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.repository

import com.d3.datacollector.model.SetAccountQuorum
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface SetAccountQuorumRepo : CrudRepository<SetAccountQuorum, Long?> {

    @Query("SELECT q FROM SetAccountQuorum q WHERE q.accountId = :accountId" +
    " ORDER BY q.transaction.block.blockCreationTime DESC")
    fun getQuorumByAccountId(accountId: String): List<SetAccountQuorum>
}
