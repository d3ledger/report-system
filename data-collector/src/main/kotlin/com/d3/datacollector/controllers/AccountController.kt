package com.d3.datacollector.controllers

import com.d3.datacollector.model.BooleanWrapper
import com.d3.datacollector.model.IntegerWrapper
import com.d3.datacollector.repository.CreateAccountRepo
import com.d3.datacollector.repository.CreateAssetRepo
import com.d3.datacollector.service.IrohaApiService
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.lang.Exception
import javax.validation.constraints.NotNull

@Controller
@RequestMapping("/iroha")
class AccountController(val accountRepo: CreateAccountRepo) {

    companion object {
        val log = KLogging().logger
    }

    @Autowired
    private lateinit var iroha: IrohaApiService

    @GetMapping("/account/exists")
    fun checkAccountExists(
        @NotNull @RequestParam accountId: String
    ): ResponseEntity<BooleanWrapper> {
        return try {
            val optional = accountRepo.findByAccountId(accountId)
            ResponseEntity.ok(BooleanWrapper(optional.isPresent))
        } catch (e: Exception) {
            log.error("Error querying account", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(BooleanWrapper(false, e.javaClass.simpleName, e.message))
        }
    }

    @GetMapping("/account/quorum")
    fun getAccountQuorum(
        @NotNull @RequestParam accountId: String
    ): ResponseEntity<IntegerWrapper> {
        return try {
            val optional = iroha.irohaQueryAccount(accountId)
            if (optional.isPresent) {
                ResponseEntity.ok(IntegerWrapper(optional.get().quorum))
            }
            ResponseEntity.ok(IntegerWrapper(0))
        } catch (e: Exception) {
            log.error("Error querying account quorum", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(IntegerWrapper(null, e.javaClass.simpleName, e.message))
        }
    }

}
