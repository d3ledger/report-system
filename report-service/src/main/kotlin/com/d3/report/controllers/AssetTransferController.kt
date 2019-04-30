package com.d3.report.controllers

import com.d3.report.model.Transfer
import com.d3.report.model.TransferReport
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
import javax.validation.constraints.NotNull

@Controller
@RequestMapping("/report/billing")
class AssetTransferController {

    @Value("\${iroha.templates.transferBilling}")
    private lateinit var transferBillingTemplate: String

    @Autowired
    private lateinit var transaferRepo: TransferAssetRepo

    @GetMapping("/transferAsset")
    fun reportBillingTransferAsset(
        @NotNull @RequestParam domain: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @RequestParam pageNum: Int = 1,
        @RequestParam pageSize: Int = 20
    ): ResponseEntity<TransferReport> {
        val report = TransferReport()
        return try {
            val page =
                transaferRepo.getDataBetween("$transferBillingTemplate$domain", from, to, PageRequest.of(pageNum - 1, pageSize))

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
}
