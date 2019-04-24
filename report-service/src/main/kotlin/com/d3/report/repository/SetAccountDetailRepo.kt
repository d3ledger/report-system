package com.d3.report.repository

import com.d3.report.model.SetAccountDetail
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface SetAccountDetailRepo : CrudRepository<SetAccountDetail, Long?> {

    @Query("SELECT sac FROM SetAccountDetail sac WHERE sac.transaction.rejected = false " +
            "and sac.account_id = :account " +
            "and sac.transaction.block.blockCreationTime Between :from and :to")
    fun getRegisteredAccountsForDomain(account:String, from: Long, to: Long, pageable: Pageable): Page<SetAccountDetail>
}
