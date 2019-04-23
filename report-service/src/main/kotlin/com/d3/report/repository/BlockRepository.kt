package com.d3.report.repository

import com.d3.report.model.Block
import org.springframework.data.repository.CrudRepository

interface BlockRepository  : CrudRepository<Block, Long?>
