package com.d3.report.controllers

import com.d3.report.model.ExchangeReport
import com.d3.report.repository.TransactionBatchRepo
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.transaction.Transactional
import javax.validation.constraints.NotNull

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/report/billing/exchange")
class ExchangeReportController(
    val trasactionBatchRepo: TransactionBatchRepo
) {

    companion object : KLogging()

    @Value("\${iroha.templates.exchangeBilling}")
    private lateinit var exchangeBillingTemplate: String

    @GetMapping("/agent")
    @Transactional
    fun reportBillingExchangeForAgent(
        @NotNull @RequestParam domain: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<ExchangeReport> {
        return try {

            val data = trasactionBatchRepo
                .getDataBetweenForBillingAccount(
                    "$exchangeBillingTemplate$domain",
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )


            ResponseEntity.ok(
                ExchangeReport()
            )
        } catch (e: Exception) {
            logger.error("Error making Custody report: ", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                ExchangeReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }
}
