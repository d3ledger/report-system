package com.d3.datacollector.repository

import com.d3.datacollector.model.State
import org.springframework.data.repository.CrudRepository
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
interface StateRepository : CrudRepository<State, Long>
