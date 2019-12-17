package com.d3.notification.repository

import com.d3.notification.domain.EthWithdrawalProofs
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface EthWithdrawalProofRepository : CrudRepository<EthWithdrawalProofs, String> {

    @Query("SELECT e FROM EthWithdrawalProofs e WHERE e.accountIdToNotify=:accountId ORDER BY e.txTime")
    fun getProofsByAccount(
        accountId: String
    ): List<EthWithdrawalProofs>
}
