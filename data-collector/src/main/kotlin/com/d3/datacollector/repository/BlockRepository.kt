package com.d3.datacollector.repository

import com.d3.datacollector.model.Block
import org.springframework.data.repository.CrudRepository
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
interface BlockRepository  : CrudRepository<Block, Long?>
