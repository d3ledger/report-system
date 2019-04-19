package com.d3.datacollector.repository

import com.d3.datacollector.model.CreateAsset
import org.springframework.data.repository.CrudRepository

interface CreateAssetRepo : CrudRepository<CreateAsset, Long?>
