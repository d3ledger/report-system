/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.controllers

import com.d3.report.model.AccountRegistration
import com.d3.report.model.RegistrationReport
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.repository.SetAccountDetailRepo
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.validation.constraints.NotNull

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/report/billing/registeredAccounts")
class RegisteredAccountsController(
    val accountDetailsRepo: SetAccountDetailRepo,
    val accountRepo: CreateAccountRepo
) {

    companion object: KLogging()

    @Value("\${iroha.templates.clientsStorage}")
    private lateinit var clientsStorageTemplate: String

    @GetMapping("/agent")
    fun reportRegistrations(
        @NotNull @RequestParam domain: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<RegistrationReport> {
        return try {
            val accountsPage = accountDetailsRepo.getRegisteredAccountsForDomain(
                "${clientsStorageTemplate}$domain",
                from,
                to,
                PageRequest.of(pageNum - 1, pageSize)
            )

            val accounts = accountsPage
                .get()
                .collect(Collectors.toList())
                .map {
                    AccountRegistration(
                        it.detailKey,
                        it.transaction?.block?.blockCreationTime
                    )
                }

            ResponseEntity.ok<RegistrationReport>(
                RegistrationReport(
                    total = accountsPage.totalElements,
                    pages = accountsPage.totalPages,
                    accounts = accounts
                )
            )
        } catch (e: Exception) {
            logger.error("Error registrations report", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                RegistrationReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    @GetMapping("/network")
    fun reportNetworkRegistrations(
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<RegistrationReport> {
        return try {
            val storageAccounts = accountRepo
                .findAccountsByName(clientsStorageTemplate.replace("@", ""))
                .map { "${it.accountName}@${it.domainId}" }

            val accDetailsList =
                accountDetailsRepo.getRegisteredAccounts(
                    storageAccounts,
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )

            val accounts = accDetailsList.get()
                .collect(Collectors.toList())
                .map {
                    AccountRegistration(
                        it.detailKey,
                        it.transaction?.block?.blockCreationTime
                    )
                }

            ResponseEntity.ok<RegistrationReport>(
                RegistrationReport(
                    total = accDetailsList.totalElements,
                    pages = accDetailsList.totalPages,
                    accounts = accounts
                )
            )
        } catch (e: Exception) {
            logger.error("Error registrations report", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                RegistrationReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

}
