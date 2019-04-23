package com.d3.datacollector.repository

import com.d3.datacollector.model.Transaction
import org.springframework.data.repository.CrudRepository

interface TransactionRepo  : CrudRepository<Transaction, Long?>
