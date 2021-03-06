/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.controllers

import com.d3.datacollector.model.AssetRate
import com.d3.datacollector.model.StringWrapper
import com.d3.datacollector.repository.RatesRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * Controller for endpoints related to asset exchange rates
 */
@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/rates")
class RatesController(
    private val assetRatesRepository: RatesRepository,
    @Value("\${server.passphrase}")
    private val passphrase: String
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
                ResponseEntity.ok(StringWrapper(asset.get().rate))
            } else {
                throw IllegalArgumentException("Asset not found")
            }
        } catch (e: IllegalArgumentException) {
            val response = StringWrapper()
            response.fill(DcExceptionStatus.ASSET_NOT_FOUND, e)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            val response = StringWrapper()
            response.fill(DcExceptionStatus.UNKNOWN_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    /**
     * POST endpoint for setting an exchange rate of the asset specified
     * @param setRateDTO JSONed body with asset name, domain and rate
     * @return [StringWrapper] with decimal exchange rate or with errors if they occur
     */
    @PostMapping("", consumes = ["application/json"])
    fun setRate(
        @RequestBody setRateDTO: SetRateDTO
    ): ResponseEntity<StringWrapper> {
        return try {
            if (setRateDTO.passphrase != passphrase) {
                throw IllegalAccessException("Wrong passphrase")
            }
            val assetId = String.format(
                "%s#%s",
                setRateDTO.assetName,
                setRateDTO.assetDomain
            )
            // to check number format
            val assetRate = BigDecimal(setRateDTO.assetRate)

            val currentAssetRecord = assetRatesRepository.findById(assetId)
            val newAssetRecord: AssetRate
            newAssetRecord = if (currentAssetRecord.isPresent) {
                currentAssetRecord.get()
            } else {
                AssetRate(assetId)
            }
            newAssetRecord.rate = assetRate.toPlainString()
            assetRatesRepository.save(newAssetRecord)

            ResponseEntity.ok(StringWrapper(setRateDTO.assetRate))
        } catch (e: Exception) {
            val response = StringWrapper()
            response.fill(DcExceptionStatus.UNKNOWN_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
}

data class SetRateDTO(
    val assetName: String,
    val assetDomain: String,
    val assetRate: String,
    // TODO change to security/auth
    val passphrase: String
)
