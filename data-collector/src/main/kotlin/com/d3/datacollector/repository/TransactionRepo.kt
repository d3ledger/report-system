/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.repository

import com.d3.datacollector.model.Transaction
import org.springframework.data.repository.CrudRepository

interface TransactionRepo  : CrudRepository<Transaction, Long?>
