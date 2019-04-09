package jp.co.soramitsu.d3.datacollector.controllers

import jp.co.soramitsu.d3.datacollector.cache.CacheRepository
import jp.co.soramitsu.d3.datacollector.model.BillingResponse
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller()
class CacheController {

    private val log = KLogging().logger

    @Autowired
    lateinit var cache: CacheRepository

    @GetMapping("/get/billing")
    fun createWallet(): ResponseEntity<BillingResponse> {
        try {
            return ResponseEntity.ok<BillingResponse>(BillingResponse(cache.getTransferBilling()))
        } catch (e: Exception) {
            log.error("Error getting Billing data", e)
            val response = BillingResponse()
            response.errorCode = e.javaClass.simpleName
            response.message = e.message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
}
