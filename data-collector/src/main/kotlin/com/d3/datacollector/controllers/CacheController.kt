/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.controllers

import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.BillingResponse
import com.d3.datacollector.model.SingleBillingResponse
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/cache")
class CacheController(
    val cache: CacheRepository
) {

    private val log = KLogging().logger

    @GetMapping("/get/billing")
    fun getAllBilling(): ResponseEntity<BillingResponse> {
        return try {
            ResponseEntity.ok<BillingResponse>(
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
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/get/billing/{domain}/{assetName}/{assetDomain}/{billingType}")
    fun getBillingForTransfer(
        @PathVariable("domain") domain: String,
        @PathVariable("assetName") assetName: String,
        @PathVariable("assetDomain") assetDomain: String,
        @PathVariable("billingType") billingType: Billing.BillingTypeEnum
    ): ResponseEntity<SingleBillingResponse> {
        return try {
            val assetId = String.format(
                "%s#%s",
                assetName,
                assetDomain
            )
            val billing = when (billingType) {
                Billing.BillingTypeEnum.TRANSFER -> cache.getTransferFee(domain, assetId)
                Billing.BillingTypeEnum.CUSTODY -> cache.getCustodyFee(domain, assetId)
                Billing.BillingTypeEnum.ACCOUNT_CREATION -> cache.getAccountCreationFee(domain, assetId)
                Billing.BillingTypeEnum.EXCHANGE -> cache.getExchangeFee(domain, assetId)
                Billing.BillingTypeEnum.WITHDRAWAL -> cache.getWithdrawalFee(domain, assetId)
                else -> throw RuntimeException("Unsupported Billing type")
            }
            ResponseEntity.ok<SingleBillingResponse>(
                SingleBillingResponse(
                    billing
                )
            )
        } catch (e: Exception) {
            log.error("Error getting Billing data", e)
            val response = SingleBillingResponse()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
}
