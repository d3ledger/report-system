package com.d3.report.repository

import com.d3.report.model.CreateAccount
import com.d3.report.model.TransferAsset
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface CreateAccountRepo : CrudRepository<CreateAccount, Long?> {

}
