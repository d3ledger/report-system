package com.d3.datacollector.repository
/*
* Copyright D3 Ledger, Inc. All Rights Reserved.
* SPDX-License-Identifier: Apache-2.0
*/
import com.d3.datacollector.model.CreateAsset
import org.springframework.data.repository.CrudRepository

interface CreateAssetRepo : CrudRepository<CreateAsset, Long?> {


}
