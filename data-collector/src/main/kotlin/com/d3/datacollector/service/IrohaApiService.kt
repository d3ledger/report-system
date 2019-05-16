/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.service

import com.d3.datacollector.utils.irohaBinaryKeyfromHex
import iroha.protocol.QryResponses
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Query
import jp.co.soramitsu.iroha.java.QueryAPI
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.security.KeyPair
import java.util.*

@Service
class IrohaApiService {
    private val irohaApi: IrohaAPI by lazy {
        IrohaAPI(URI(toriiAddress))
    }
    private val queryApi: QueryAPI by lazy {
        QueryAPI(irohaApi, userId, keyPair)
    }
    private val keyPair: KeyPair by lazy {
        Ed25519Sha3.keyPairFromBytes(
            irohaBinaryKeyfromHex(privateKey),
            irohaBinaryKeyfromHex(publicKey)
        )
    }

    @Value("\${iroha.toriiAddress}")
    lateinit var toriiAddress: String
    @Value("\${iroha.user.privateKeyHex}")
    private lateinit var privateKey: String
    @Value("\${iroha.user.publicKeyHex}")
    private lateinit var publicKey: String
    @Value("\${iroha.user.id}")
    private lateinit var userId: String

    @Synchronized
    fun irohaQueryAccount(accountId: String): Optional<QryResponses.Account> {
        val response = queryApi?.getAccount(accountId)
        if(response?.hasAccount() == true) {
            return Optional.of(response.account)
        }
        return Optional.empty()
    }

    fun irohaBlockQuery(
        newRequestNumber: Long,
        newBlock: Long
    ): QryResponses.QueryResponse {
        val q = Query.builder(userId, newRequestNumber + 1)
            .getBlock(newBlock)
            .buildSigned(keyPair)
        val response = irohaApi!!.query(q)
        return response
    }
}
