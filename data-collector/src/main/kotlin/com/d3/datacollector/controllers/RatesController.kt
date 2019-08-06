/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.controllers

import com.d3.datacollector.model.StringWrapper
import com.d3.datacollector.repository.RatesRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Controller for endpoints related to asset exchange rates
 */
@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/rates")
class RatesController(
    val assetRatesRepository: RatesRepository
) {

    /**
     * GET endpoint for querying an exchange rate of the asset specified
     * @param assetName Iroha name of the asset
     * @param assetDomain Iroha domain of the asset
     * @return [StringWrapper] with decimal exchange rate or with errors if they occur
     */
    @GetMapping("/{assetName}/{assetDomain}")
    fun getRate(
        @PathVariable("assetName") assetName: String,
        @PathVariable("assetDomain") assetDomain: String
    ): ResponseEntity<StringWrapper> {
        return try {
            val assetId = String.format(
                "%s#%s",
                assetName,
                assetDomain
            )
            val asset = assetRatesRepository.findById(assetId)
            if (asset.isPresent && !asset.get().rate.isNullOrEmpty()) {
                ResponseEntity.ok<StringWrapper>(StringWrapper(asset.get().rate))
            } else {
                throw IllegalArgumentException("Asset not found")
            }
        } catch (e: Exception) {
            val response = StringWrapper()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            ResponseEntity.ok<StringWrapper>(response)
        }
    }
}
