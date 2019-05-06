package com.d3.report.controllers

import com.d3.report.model.AccountCustody
import com.d3.report.model.CustodyReport
import com.d3.report.model.RegistrationReport
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.repository.TransferAssetRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.math.BigDecimal
import java.util.stream.Collectors
import javax.validation.constraints.NotNull

@Controller
@RequestMapping("/report/billing/custody")
class CustodyController {

    @Autowired
    private lateinit var transaferRepo: TransferAssetRepo
    @Autowired
    private lateinit var accountRepo: CreateAccountRepo

    @GetMapping("agent")
    fun reportBillingTransferAsset(
        @NotNull @RequestParam domain: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @RequestParam pageNum: Int = 1,
        @RequestParam pageSize: Int = 20
    ): ResponseEntity<CustodyReport> {
        return try {
            val accountsPage = accountRepo.getDomainAccounts(domain, PageRequest.of(pageNum - 1, pageSize))
            accountsPage.forEach { account ->
                var calculatedPages = 1
                val custodyFees = HashMap<String, AccountCustody>()
                do {
                    val transfersPage = transaferRepo.getDataBetween(
                        "${account.accountName}@",
                        from,
                        to,
                        PageRequest.of(pageNum - 1, 200)
                    )
                    transfersPage
                        .get()
                        .forEach { asset ->
                            val bomba = custodyFees.computeIfAbsent(
                                account.accountName!!,
                                { AccountCustody(account.accountName!!) })
                            val custody = bomba.assetCustody.computeIfAbsent(asset.assetId!!, { BigDecimal("0") })
                            kbl
                        }
                } while (++calculatedPages - transfersPage.totalPages < 0)
            }

            ResponseEntity.status(HttpStatus.CONFLICT).body(
                CustodyReport(
                    total = accountsPage.totalElements,
                    pages = accountsPage.totalPages
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                CustodyReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }
}
