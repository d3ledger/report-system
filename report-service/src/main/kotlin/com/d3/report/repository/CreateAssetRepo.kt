/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.repository

import com.d3.report.model.CreateAsset
import org.springframework.data.repository.CrudRepository

interface CreateAssetRepo : CrudRepository<CreateAsset, Long?>


