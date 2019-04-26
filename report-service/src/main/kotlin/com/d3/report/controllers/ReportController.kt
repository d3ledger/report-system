package com.d3.report.controllers

import com.d3.report.model.AccountRegistration
import com.d3.report.model.RegistrationReport
import com.d3.report.model.Transfer
import com.d3.report.model.TransferReport
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.repository.SetAccountDetailRepo
import com.d3.report.repository.TransferAssetRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.stream.Collectors

@Controller
@RequestMapping("/report")
class ReportController {

    @Value("\${iroha.templates.transferBilling}")
    private lateinit var transferBillingTemplate: String
    @Value("\${iroha.templates.clientsStorage}")
    private lateinit var clientsStorageTemplate: String

    @Autowired
    private lateinit var transaferRepo: TransferAssetRepo
    @Autowired
    private lateinit var accountDetailsRepo: SetAccountDetailRepo
    @Autowired
    private lateinit var accountRepo: CreateAccountRepo


    @GetMapping("/billing/transferAsset")
    fun reportBillingTransferAsset(
        @RequestParam from: Long,
        @RequestParam to: Long,
        @RequestParam pageNum: Int = 1,
        @RequestParam pageSize: Int = 20

    ): ResponseEntity<TransferReport> {
        val report = TransferReport()
        return try {
            val page =
                transaferRepo.getDataBetween(transferBillingTemplate, from, to, PageRequest.of(pageNum - 1, pageSize))

            report.pages = page.totalPages
            report.total = page.totalElements

            page.get().collect(Collectors.toList())
                .groupBy { it.transaction.id }
                .forEach {
                    var transfer = Transfer()
                    it.value.forEach {
                        if (it.destAccountId?.contains(transferBillingTemplate) == true) {
                            transfer.fee = it
                        } else {
                            transfer.transfer = it
                        }
                    }
                    report.transfers.add(transfer)
                }
            ResponseEntity.ok<TransferReport>(report)
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                TransferReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    @GetMapping("/billing/agent/registeredAccounts")
    fun reportRegistrations(
        @RequestParam domain: String,
        @RequestParam from: Long,
        @RequestParam to: Long,
        @RequestParam pageNum: Int = 1,
        @RequestParam pageSize: Int = 20
    ): ResponseEntity<RegistrationReport> {
        return try {
            val accountsPage = accountDetailsRepo.getRegisteredAccountsForDomain(
                "${clientsStorageTemplate}$domain",
                from,
                to,
                PageRequest.of(pageNum - 1, pageSize)
            )

            val dataList = accountsPage
                .get()
                .collect(Collectors.toList())

            val accounts = dataList.map {
                AccountRegistration(
                    it.detailKey,
                    it.transaction.block?.blockCreationTime
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
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                RegistrationReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    @GetMapping("/billing/network/registeredAccounts")
    fun reportNetworkRegistrations(
        @RequestParam from: Long,
        @RequestParam to: Long,
        @RequestParam pageNum: Int = 1,
        @RequestParam pageSize: Int = 20
    ): ResponseEntity<RegistrationReport> {
        return try {
            val accountsCreated = accountRepo.findAccountsByName(clientsStorageTemplate.replace("@",""))
            val storageAccounts = accountsCreated.map { "${it.accountName}@${it.domainId}" }

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
                        it.transaction.block?.blockCreationTime
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
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                RegistrationReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }
}
