/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.controllers

import com.d3.datacollector.model.StringWrapper
import com.d3.datacollector.service.EthGasPriceProvider
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Controller for endpoints related to gas price
 */
@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/gas")
class GasController(
    private val ethGasPriceProvider: EthGasPriceProvider
) {

    /**
     * GET endpoint for querying relevant gas price
     * @return [StringWrapper] with decimal gas price or with errors if they occur
     */
    @GetMapping("")
    fun getGasPrice(): ResponseEntity<StringWrapper> {
        return try {
            val gasPrice = ethGasPriceProvider.getGasPrice()
            if (!gasPrice.isNullOrEmpty()) {
                ResponseEntity.ok(StringWrapper(gasPrice))
            } else {
                throw IllegalStateException("Gas price cannot be retrieved")
            }
        } catch (e: Exception) {
            val response = StringWrapper()
            response.fill(DcExceptionStatus.UNKNOWN_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
}
