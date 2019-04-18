package com.d3.datacollector.repository

import com.d3.datacollector.model.Block
import org.springframework.data.repository.CrudRepository

interface BlockRepository  : CrudRepository<Block, Long?>
