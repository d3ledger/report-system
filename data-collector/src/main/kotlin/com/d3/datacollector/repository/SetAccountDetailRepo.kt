/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.repository

import com.d3.datacollector.model.SetAccountDetail
import org.springframework.data.repository.CrudRepository

interface SetAccountDetailRepo : CrudRepository<SetAccountDetail, Long?>
