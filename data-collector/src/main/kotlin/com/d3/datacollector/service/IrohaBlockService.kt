/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.service

import jp.co.soramitsu.iroha.java.QueryAPI
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class IrohaBlockService {

    @Autowired
    private lateinit var queryAPI: QueryAPI

    fun irohaBlockQuery(blockHeight: Long) = queryAPI.getBlock(blockHeight)
}
