package com.d3.report.controllers

import com.d3.report.model.Transfer
import com.d3.report.model.TransferReport
import com.d3.report.repository.TransferAssetRepo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.stream.Collectors

@Controller
@RequestMapping("/report")
class ReportController {

    @Autowired
    private lateinit var transaferRepo: TransferAssetRepo
    @Value("\${iroha.transferBillingTemplate}")
    private lateinit var transferfeeAccTemplate: String

    @GetMapping("/billing/transferAsset")
    fun reportBillingTransferAsset(
        @RequestParam from: Long,
        @RequestParam to: Long,
        @RequestParam pageNum: Int = 1,
        @RequestParam pageSize: Int = 20

    ): ResponseEntity<TransferReport> {
        val report = TransferReport()
        val page = transaferRepo.getDataBetween(from, to, PageRequest.of(pageNum - 1, pageSize))

        report.pages = page.totalPages
        report.total = page.totalElements

        page.get().collect(Collectors.toList())
            .groupBy { it.transaction.id }
            .forEach {
                var transfer = Transfer()
                it.value.forEach {
                    if (it.destAccountId?.contains(transferfeeAccTemplate) == true) {
                        transfer.fee = it
                    } else {
                        transfer.transfer = it
                    }
                }
                report.transfers.add(transfer)
            }

        return ResponseEntity.ok<TransferReport>(report)
    }
}
