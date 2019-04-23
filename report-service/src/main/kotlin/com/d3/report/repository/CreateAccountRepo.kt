package com.d3.report.repository

import com.d3.report.model.CreateAccount
import org.springframework.data.repository.CrudRepository

interface CreateAccountRepo : CrudRepository<CreateAccount, Long?>
