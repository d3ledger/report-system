package com.d3.report.repository

import com.d3.report.model.Transaction
import org.springframework.data.repository.CrudRepository

interface TransactionRepo  : CrudRepository<Transaction, Long?>
