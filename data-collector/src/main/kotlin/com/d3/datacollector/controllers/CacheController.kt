package com.d3.datacollector.controllers

import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.BillingResponse
import com.d3.datacollector.model.SingleBillingResponse
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/cache")
class CacheController {

    private val log = KLogging().logger

    @Autowired
    lateinit var cache: CacheRepository

    @GetMapping("/get/billing")
    fun getAllBilling(): ResponseEntity<BillingResponse> {
        try {
            return ResponseEntity.ok<BillingResponse>(
                BillingResponse(
                    cache.getTransferFee(),
                    cache.getCustodyFee(),
                    cache.getAccountCreationFee(),
                    cache.getExchangeFee(),
                    cache.getWithdrawalFee()
                )
            )
        } catch (e: Exception) {
            log.error("Error getting Billing data", e)
            val response = BillingResponse()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/get/billing/{domain}/{asset}/{billingType}")
    fun getBillingForTransfer(
        @PathVariable("domain") domain: String,
        @PathVariable("asset") asset: String,
        @PathVariable("billingType") billingType: Billing.BillingTypeEnum
    ): ResponseEntity<SingleBillingResponse> {
        try {
            val billing: Billing
            when (billingType) {
                Billing.BillingTypeEnum.TRANSFER -> billing = cache.getTransferFee(domain, asset)
                Billing.BillingTypeEnum.CUSTODY -> billing = cache.getCustodyFee(domain, asset)
                Billing.BillingTypeEnum.ACCOUNT_CREATION -> billing = cache.getAccountCreationFee(domain, asset)
                Billing.BillingTypeEnum.EXCHANGE -> billing = cache.getExchangeFee(domain, asset)
                Billing.BillingTypeEnum.WITHDRAWAL -> billing = cache.getWithdrawalFee(domain, asset)
                else -> throw RuntimeException("Unsupported Billing type")
            }
            return ResponseEntity.ok<SingleBillingResponse>(
                SingleBillingResponse(
                    billing
                )
            )
        } catch (e: Exception) {
            log.error("Error getting Billing data", e)
            val response = SingleBillingResponse()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
}