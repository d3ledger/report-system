/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.controllers

import com.d3.datacollector.cache.CacheRepository
import com.d3.datacollector.model.Billing
import com.d3.datacollector.model.BillingResponse
import com.d3.datacollector.model.PostBillingRequestDTO
import com.d3.datacollector.model.SingleBillingResponse
import com.d3.datacollector.repository.CreateAssetRepo
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/cache")
class CacheController(
    private val cache: CacheRepository,
    private val assetRepo: CreateAssetRepo
) {

    @GetMapping("/get/billing")
    @ResponseBody
    fun getAllBilling(): ResponseEntity<BillingResponse> {
        return try {
            ResponseEntity.ok(
                BillingResponse(
                    cache.getTransferBilling(),
                    cache.getCustodyBilling(),
                    cache.getAccountCreationBilling(),
                    cache.getExchangeBilling(),
                    cache.getWithdrawalBilling()
                )
            )
        } catch (e: Exception) {
            logger.error("Error getting Billing data", e)
            val response = BillingResponse()
            response.fill(DcExceptionStatus.UNKNOWN_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/get/billing/{domain}/{assetName}/{assetDomain}/{billingType}")
    @ResponseBody
    fun getConcreteBilling(
        @PathVariable("domain") domain: String,
        @PathVariable("assetName") assetName: String,
        @PathVariable("assetDomain") assetDomain: String,
        @PathVariable("billingType") billingType: Billing.BillingTypeEnum
    ): ResponseEntity<SingleBillingResponse> {
        return try {
            val assetId = "$assetName#$assetDomain"
            val billingMap = when (billingType) {
                Billing.BillingTypeEnum.TRANSFER -> cache.getTransferBilling(domain, assetId)
                Billing.BillingTypeEnum.CUSTODY -> cache.getCustodyBilling(domain, assetId)
                Billing.BillingTypeEnum.ACCOUNT_CREATION -> cache.getAccountCreationBilling(domain, assetId)
                Billing.BillingTypeEnum.EXCHANGE -> cache.getExchangeBilling(domain, assetId)
                Billing.BillingTypeEnum.WITHDRAWAL -> cache.getWithdrawalBilling(domain, assetId)
                else -> throw RuntimeException("Unsupported Billing type")
            }
            val asset = assetRepo.findByAssetId(assetId)
                .orElseThrow { IllegalArgumentException("No such asset: $assetId") }
            ResponseEntity.ok(SingleBillingResponse(billingMap, asset.decimalPrecision))
        } catch (e: IllegalArgumentException) {
            val response = SingleBillingResponse()
            response.fill(DcExceptionStatus.ASSET_NOT_FOUND, e)
            ResponseEntity.ok(response)
        } catch (e: IllegalStateException) {
            val response = SingleBillingResponse()
            response.fill(DcExceptionStatus.FEE_NOT_SET, e)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error getting Billing data", e)
            val response = SingleBillingResponse()
            response.fill(DcExceptionStatus.UNKNOWN_ERROR, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @PostMapping("/post/billing", consumes = [APPLICATION_JSON_VALUE])
    @ResponseBody
    fun postConcreteBilling(@RequestBody postBillingRequest: PostBillingRequestDTO): ResponseEntity<SingleBillingResponse> =
        getConcreteBilling(
            postBillingRequest.domain,
            postBillingRequest.assetName,
            postBillingRequest.assetDomain,
            postBillingRequest.billingType
        )

    companion object : KLogging()
}
