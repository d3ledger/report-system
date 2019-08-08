/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.controllers

import com.d3.report.model.AssetSumReportReport
import com.d3.report.model.TransferAsset
import com.d3.report.repository.CLIENT_DOMAIN
import com.d3.report.repository.TransferAssetRepo
import mu.KLogging
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.math.BigDecimal
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
     * Calculates sum of given asset in the D3 domain
     */
    @GetMapping("/iroha")
    fun sumIrohaAsset(
        @NotNull @RequestParam assetId: String
    ): ResponseEntity<AssetSumReportReport> {
        return try {
            var totalSum = BigDecimal(0)
            transferRepo.getAllClientTransfersForAsset(assetId, PageRequest.of(0, 20))
                .get().forEach { transfer ->
                    if (isDepositTransfer(transfer)) {
                        totalSum += transfer.amount!!
                    } else if (isWithdrawalTransfer(transfer)) {
                        totalSum -= transfer.amount!!
                    }
                }
            ResponseEntity.ok(
                AssetSumReportReport(totalSum = totalSum, assetId = assetId)
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

    /**
     * Checks if transfer is a deposit
     * @param transferAsset - transfer to check
     * @return true if deposit
     */
    private fun isDepositTransfer(transferAsset: TransferAsset) =
        !transferAsset.srcAccountId!!.endsWith("@$CLIENT_DOMAIN") && transferAsset.destAccountId!!.endsWith("@$CLIENT_DOMAIN")

    /**
     * Checks if transfer is a withdrawal
     * @param transferAsset - transfer to check
     * @return true if withdrawal
     */
    private fun isWithdrawalTransfer(transferAsset: TransferAsset) =
        transferAsset.srcAccountId!!.endsWith("@$CLIENT_DOMAIN") && !transferAsset.destAccountId!!.endsWith("@$CLIENT_DOMAIN")


    companion object : KLogging()
}
