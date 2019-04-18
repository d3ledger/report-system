package com.d3.datacollector.repository

import com.d3.datacollector.model.TransferAsset
import org.springframework.data.repository.CrudRepository

interface TransferAssetRepo  : CrudRepository<TransferAsset, Long?>
