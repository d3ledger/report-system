package com.d3.report.repository

import com.d3.report.model.CreateAsset
import org.springframework.data.repository.CrudRepository

interface CreateAssetRepo : CrudRepository<CreateAsset, Long?>
