package com.d3.report.controllers

import com.d3.report.model.ExchangeReport
import com.d3.report.model.TransactionBatchEntity
import com.d3.report.model.TransferAsset
import com.d3.report.repository.TransactionBatchRepo
import com.d3.report.repository.TransactionRepo
import com.d3.report.repository.TransferAssetRepo
import com.d3.report.utils.getDomain
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
import java.sql.ResultSet
import java.util.stream.Collectors
import javax.persistence.EntityManager
import javax.transaction.Transactional
import javax.validation.constraints.NotNull

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/report/billing/exchange")
class ExchangeReportController(
    val trasactionBatchRepo: TransactionBatchRepo,
    val transactionRepo: TransactionRepo,
    val em: EntityManager
) {

    companion object : KLogging()

    @Value("\${iroha.templates.exchangeBilling}")
    private lateinit var exchangeBillingTemplate: String
    val queryString = "SELECT * FROM Command c WHERE c.transaction_id = ?1"

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

            val page = trasactionBatchRepo
                .getDataBetweenForBillingAccount(
                    "$exchangeBillingTemplate$domain",
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )
            fillNotFetchedBatchData(page)
            prepeareResponse(page)
        } catch (e: Exception) {
            logger.error("Error making report: ", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                ExchangeReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    @GetMapping("/customer")
    @Transactional
    fun reportBillingExchangeForCustomer(
        @NotNull @RequestParam accountId: String,
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<ExchangeReport> {
        return try {
            val exchangeBillingAccountId = "$exchangeBillingTemplate${getDomain(accountId)}"
            val page = trasactionBatchRepo
                .getDataBetweenForBillingAccountAndCustomer(
                    accountId,
                    exchangeBillingAccountId,
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )

            fillNotFetchedBatchData(page)
            prepeareResponse(page)
        } catch (e: Exception) {
            logger.error("Error making report: ", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                ExchangeReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    @GetMapping("/system")
    @Transactional
    fun reportBillingExchangeForTheSystem(
        @NotNull @RequestParam from: Long,
        @NotNull @RequestParam to: Long,
        @NotNull @RequestParam pageNum: Int = 1,
        @NotNull @RequestParam pageSize: Int = 20
    ): ResponseEntity<ExchangeReport> {
        return try {
            val page = trasactionBatchRepo
                .getDataBetweenForbillingAccountTemplate(
                    exchangeBillingTemplate,
                    from,
                    to,
                    PageRequest.of(pageNum - 1, pageSize)
                )

            fillNotFetchedBatchData(page)
            prepeareResponse(page)
        } catch (e: Exception) {
            logger.error("Error making report: ", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                ExchangeReport(
                    code = e.javaClass.simpleName,
                    message = e.message
                )
            )
        }
    }

    private fun prepeareResponse(page: Page<TransactionBatchEntity>): ResponseEntity<ExchangeReport> {
        val batchList = page.get().collect(Collectors.toList())
        return ResponseEntity.ok(
            ExchangeReport(
                batchList,
                page.totalElements,
                page.totalPages
            )
        )
    }

    private fun fillNotFetchedBatchData(page: Page<TransactionBatchEntity>) {
        page.get().forEach { batch ->
            batch.transactions = transactionRepo.getTransactionsByBatchId(batch.id!!)
            batch.transactions.forEach { tx ->
                val query = em.createNativeQuery(queryString, TransferAsset().javaClass)
                query.setParameter(1, tx.id)
                query.getResultList().forEach {
                    tx.commands.add(it as TransferAsset)
                }
            }
        }
    }

}
