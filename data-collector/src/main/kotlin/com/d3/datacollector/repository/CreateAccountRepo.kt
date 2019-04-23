package com.d3.datacollector.repository

import com.d3.datacollector.model.CreateAccount
import org.springframework.data.repository.CrudRepository

interface CreateAccountRepo : CrudRepository<CreateAccount, Long?>
