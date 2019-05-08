package com.d3.datacollector.repository

import com.d3.datacollector.model.CreateAsset
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
interface CreateAssetRepo : CrudRepository<CreateAsset, Long?> {


}
