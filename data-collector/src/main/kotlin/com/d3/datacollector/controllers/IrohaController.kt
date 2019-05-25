/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.controllers

import com.d3.datacollector.model.AssetsResponse
import com.d3.datacollector.model.BooleanWrapper
import com.d3.datacollector.model.IntegerWrapper
import com.d3.datacollector.model.SetAccountDetail
import com.d3.datacollector.repository.CreateAccountRepo
import com.d3.datacollector.repository.SetAccountDetailRepo
import com.d3.datacollector.repository.SetAccountQuorumRepo
import com.d3.datacollector.service.IrohaApiService
import iroha.protocol.QryResponses
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.lang.Exception
import java.util.HashMap
import javax.validation.constraints.NotNull

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/iroha")
class IrohaController(
    val accountRepo: CreateAccountRepo,
    val quorumRepo: SetAccountQuorumRepo,
    val accountDetailRepo: SetAccountDetailRepo
) {

    companion object {
        val log = KLogging().logger
    }

    val assetList = "assets_list"
    val currencyAccount = "$assetList@currency"
    val securityAccount = "$assetList@security"
    val utilityAccount = "$assetList@utility"
    val privateAccount = "$assetList@private"


    @GetMapping("/asset/getAll")
    fun GetAllAssets(): ResponseEntity<AssetsResponse> {
        return try {
            val currencies = accountDetailRepo.getAllDetailsForAccountId(currencyAccount)
            val securities = accountDetailRepo.getAllDetailsForAccountId(securityAccount)
            val utility = accountDetailRepo.getAllDetailsForAccountId(utilityAccount)
            val private = accountDetailRepo.getAllDetailsForAccountId(privateAccount)

            val response = AssetsResponse(
                getMapFromAccountDetails(currencies),
                getMapFromAccountDetails(securities),
                getMapFromAccountDetails(utility),
                getMapFromAccountDetails(private)
            )
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            log.error("Error querying assets", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body( AssetsResponse(
                errorCode = e.javaClass.simpleName,
                errorMessage = e.message))
        }
    }

    private fun getMapFromAccountDetails(details: List<SetAccountDetail>): Map<String?, String?> {
        return details.map { it.detailKey to it.detailValue }.toMap()
    }

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
            val optional = accountRepo.findByAccountId(accountId)
            if (optional.isPresent) {
                val quorumList = quorumRepo.getQuorumByAccountId(accountId)
                if (quorumList.isNotEmpty()) {
                    ResponseEntity.ok(IntegerWrapper(quorumList[0].quorum))
                } else {
                    ResponseEntity.ok(IntegerWrapper(1))
                }
            } else {
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(IntegerWrapper(null, "BAD_REQUEST", "Account doesn't exist"))
            }
        } catch (e: Exception) {
            log.error("Error querying account quorum", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(IntegerWrapper(null, e.javaClass.simpleName, e.message))
        }
    }
}
