/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.controllers

import com.d3.report.model.Transfer
import com.d3.report.model.TransferAsset
import com.d3.report.model.TransferReport
import com.d3.report.repository.TransferAssetRepo
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.stream.Collectors
import javax.validation.constraints.NotNull

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/report/billing/transferAsset")
class AssetTransferController(
    val transferRepo: TransferAssetRepo
) {

    companion object : KLogging()

    @Value("\${iroha.templates.transferBilling}")
    private lateinit var transferBillingTemplate: String

    @GetMapping("/account")
    fun reportCustomerTransferAssetBilling(
        @NotNull @RequestParam accountId: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20,
        @RequestParam assetId: String? = null
    ): ResponseEntity<TransferReport> {
        return try {
            val report = TransferReport()
            val domain = accountId.substring(accountId.indexOf('@') + 1)
            val page: Page<TransferAsset>
            if(assetId == null) {
              page =
                transferRepo.getDataBetweenForBillingAccount(
                    accountId,
                    "$transferBillingTemplate$domain",
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )
            } else {
                page = transferRepo.getDataBetweenForAssetOfAccount(assetId,  accountId,
                    "$transferBillingTemplate$domain",
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )
            }

            report.pages = page.totalPages
            report.total = page.totalElements

            mapTransfersWithItsCommissions(page, report)
          
            ResponseEntity.ok<TransferReport>(report)
        } catch (e: Exception) {
            logger.error("Error creating transfer billing report for customer.", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                TransferReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    @GetMapping("/domain")
    fun reportAgentBillingTransferAsset(
        @NotNull @RequestParam domain: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<TransferReport> {
        val report = TransferReport()
        return try {
            val page =
                transferRepo.getDataBetweenForBillingAccount(
                    "$transferBillingTemplate$domain",
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )

            report.pages = page.totalPages
            report.total = page.totalElements

            mapTransfersWithItsCommissions(page, report)

            ResponseEntity.ok<TransferReport>(report)
        } catch (e: Exception) {
            logger.error("Error creating transfer billing report for agent.", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                TransferReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    @GetMapping("/system")
    fun reportSystemBillingTransferAsset(
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<TransferReport> {
        val report = TransferReport()
        return try {
            val page =
                transferRepo.getDataBetweenForBillingAccountTemplate(
                    transferBillingTemplate,
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )

            report.pages = page.totalPages
            report.total = page.totalElements

            mapTransfersWithItsCommissions(page, report)

            ResponseEntity.ok<TransferReport>(report)
        } catch (e: Exception) {
            logger.error("Error creating transfer billing report for agent.", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                TransferReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    private fun mapTransfersWithItsCommissions(
        page: Page<TransferAsset>,
        report: TransferReport
    ) {
        page.get().collect(Collectors.toList())
            .filter { it.transaction != null }
            .groupBy { it.transaction?.id }
            .forEach {
                val transfer = Transfer()
                it.value.forEach {
                    if (it.destAccountId?.contains(transferBillingTemplate) == true) {
                        transfer.fee = it
                    } else {
                        transfer.transfer = it
                    }
                }
                report.transfers.add(transfer)
            }
    }
}
