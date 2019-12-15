package com.d3.notification.repository

import com.d3.notification.domain.EthWithdrawalProofs
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional;

interface EthWithdrawalProofRepository : CrudRepository<EthWithdrawalProofs, String> {

    @Query("SELECT e FROM EthWithdrawalProofs e WHERE e.accountIdToNotify=:accountId AND e.blockNum>=:sinceBlockNum AND e.ack=false ORDER BY e.txTime")
    fun getNoAckProofsByAccount(
        accountId: String,
        sinceBlockNum: Long
    ): List<EthWithdrawalProofs>

    @Transactional
    @Modifying
    @Query("UPDATE EthWithdrawalProofs SET ack=true WHERE id=:proofEventId")
    fun ackProofByEventId(proofEventId: String)
}
