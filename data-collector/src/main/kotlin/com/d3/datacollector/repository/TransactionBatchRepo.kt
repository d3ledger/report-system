package com.d3.datacollector.repository

import com.d3.datacollector.model.TransactionBatchEntity
import org.springframework.data.repository.CrudRepository

interface TransactionBatchRepo : CrudRepository<TransactionBatchEntity, Long>
