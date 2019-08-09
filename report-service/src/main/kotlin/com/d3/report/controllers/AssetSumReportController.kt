/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.controllers

import com.d3.report.model.AssetSumReportReport
import com.d3.report.repository.TransferAssetRepo
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.validation.constraints.NotNull

/**
 * Controller that provides asset sum reports
 */
@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/report/asset/sum")
class AssetSumReportController(
    private val transferRepo: TransferAssetRepo
) {

    /**
     * Calculates sum of asset
     */
    @GetMapping("/iroha")
    fun sumIrohaAsset(
        @NotNull @RequestParam assetId: String,
        @NotNull @RequestParam domainId: String
    ): ResponseEntity<AssetSumReportReport> {
        return try {
            ResponseEntity.ok(
                AssetSumReportReport(
                    totalSum = transferRepo.getSumAsset(assetId, "@$domainId"),
                    assetId = assetId
                )
            )
        } catch (e: Exception) {
            logger.error("Error making report: ", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                AssetSumReportReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    companion object : KLogging()
}
