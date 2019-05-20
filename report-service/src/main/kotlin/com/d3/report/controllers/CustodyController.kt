/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.controllers

import com.d3.report.model.AccountCustody
import com.d3.report.model.Billing
import com.d3.report.model.CustodyReport
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.service.CustodyService
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.stream.Collectors
import javax.transaction.Transactional
import javax.validation.constraints.NotNull
@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/report/billing/custody")
class CustodyController(
    val accountRepo: CreateAccountRepo,
    val custodyService: CustodyService
) {

    companion object : KLogging()

    /**
     * Add from parameter and saving of daily snapshots.
     * To calculate fees for a period on a finished dayly basis. Not to recalculate all values for every request.
     */
    @GetMapping("/agent")
    @Transactional
    fun reportBillingTransferAsset(
        @NotNull @RequestParam domain: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<CustodyReport> {
        val billingStore = HashMap<String, Billing>()
        return try {
            val accountsPage =
                accountRepo.getDomainAccounts(domain, PageRequest.of(pageNum - 1, pageSize))

            /*
                 Collection with custody Fee
            */
            val custodyFees = HashMap<String, AccountCustody>()
            custodyService.processAccounts(
                accountsPage,
                pageNum,
                billingStore,
                from,
                to,
                custodyFees
            )

            ResponseEntity.ok(
                CustodyReport(
                    accounts = custodyFees.values.stream().collect(Collectors.toList()),
                    total = accountsPage.totalElements,
                    pages = accountsPage.totalPages
                )
            )
        } catch (e: Exception) {
            logger.error("Error making Custody report: ", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                CustodyReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

}
