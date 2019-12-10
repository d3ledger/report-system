/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.controllers

import com.d3.commons.sidechain.iroha.util.impl.IrohaQueryHelperImpl
import com.d3.datacollector.model.*
import com.d3.datacollector.repository.CreateAccountRepo
import com.d3.datacollector.repository.CreateAssetRepo
import com.d3.datacollector.repository.SetAccountDetailRepo
import com.d3.datacollector.repository.SetAccountQuorumRepo
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.validation.constraints.NotNull

@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@Controller
@RequestMapping("/iroha")
class IrohaController(
    private val accountRepo: CreateAccountRepo,
    private val quorumRepo: SetAccountQuorumRepo,
    private val accountDetailRepo: SetAccountDetailRepo,
    private val assetRepo: CreateAssetRepo,
    private val queryHelper: IrohaQueryHelperImpl
) {

    companion object : KLogging()

    private final val assetList = "assets_list"
    val currencyAccount = "$assetList@currency"
    val securityAccount = "$assetList@security"
    val utilityAccount = "$assetList@utility"
    val privateAccount = "$assetList@private"

    @GetMapping("eth/masterContract")
    fun getMasterContractAddress(): ResponseEntity<StringWrapper> {
        return try {
            val detail = accountDetailRepo.getAccountDetail(
                accountId = "notary@notary",
                detailKey = "eth_master_address",
                creatorId = "superuser@bootstrap"
            )
            if (detail != null) {
                ResponseEntity.ok(StringWrapper(detail.detailValue))
            } else {
                throw IllegalStateException("There is no master contract address")
            }
        } catch (e: Exception) {
            logger.error("Error querying master contract address", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                StringWrapper(
                    errorCode = e.javaClass.simpleName,
                    errorMessage = e.message
                )
            )
        }
    }

    @GetMapping("/asset/getAll")
    fun getAllAssets(): ResponseEntity<AssetsResponse> {
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
            logger.error("Error querying assets", e)
            ResponseEntity.status(HttpStatus.CONFLICT).body(
                AssetsResponse(
                    errorCode = e.javaClass.simpleName,
                    errorMessage = e.message
                )
            )
        }
    }

    @GetMapping("/asset/precision/{assetId}")
    fun getAssetPrecision(
        @PathVariable("assetId") assetId: String
    ): ResponseEntity<IntegerWrapper> {
        return try {
            val optional = assetRepo.findByAssetId(assetId)
            if (optional.isPresent) {
                val precision = optional.get().decimalPrecision
                ResponseEntity.ok(IntegerWrapper(precision))
            } else {
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(IntegerWrapper(null, "BAD_REQUEST", "Asset doesn't exist"))
            }
        } catch (e: Exception) {
            logger.error("Error querying asset precision", e)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(IntegerWrapper(null, e.javaClass.simpleName, e.message))
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
            logger.error("Error querying account", e)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BooleanWrapper(false, e.javaClass.simpleName, e.message))
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
            logger.error("Error querying account quorum", e)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(IntegerWrapper(null, e.javaClass.simpleName, e.message))
        }
    }

    @GetMapping("/health")
    fun getIrohaHealthStatus(): ResponseEntity<BooleanWrapper> {
        return queryHelper.getBlock(1).fold({
            ResponseEntity.ok(BooleanWrapper(true))
        }, { e ->
            logger.error("Error getting Iroha health status", e)
            ResponseEntity.ok(BooleanWrapper(false))
        })
    }
}
