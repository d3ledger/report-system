package com.d3.datacollector.repository

import com.d3.datacollector.model.CreateAsset
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface CreateAssetRepo : CrudRepository<CreateAsset, Long?> {


}
