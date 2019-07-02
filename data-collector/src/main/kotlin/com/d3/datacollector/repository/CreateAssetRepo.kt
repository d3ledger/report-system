/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.repository

import com.d3.datacollector.model.CreateAsset
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface CreateAssetRepo : CrudRepository<CreateAsset, Long?> {

    @Query("SELECT a FROM CreateAsset a WHERE CONCAT(a.assetName,'#',a.domainId) = :assetId ")
    fun findByAssetId(assetId: String): Optional<CreateAsset>
}
