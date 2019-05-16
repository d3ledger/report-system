/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.util.*
import javax.persistence.*
import javax.persistence.PreUpdate
import javax.persistence.PrePersist
import kotlin.collections.ArrayList

@Entity
@Table(name = "state")
data class State(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var title: String = "",
    var value: String = ""
)

@Entity
@Table(name = "billing")
data class Billing(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Long? = null,
    @JsonIgnore
    val accountId: String = "",
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    val billingType: BillingTypeEnum = BillingTypeEnum.TRANSFER,
    @JsonIgnore
    val asset: String = "",
    var feeFraction: BigDecimal = BigDecimal("0.015"),
    var created: Long = 0L,
    var updated: Long = 0L
) {
    enum class BillingTypeEnum {
        @JsonProperty("transfer")
        TRANSFER,
        @JsonProperty("custody")
        CUSTODY,
        @JsonProperty("account_creation")
        ACCOUNT_CREATION,
        @JsonProperty("exchange")
        EXCHANGE,
        @JsonProperty("withdrawal")
        WITHDRAWAL,
        @JsonProperty("not_found")
        NOT_FOUND
    }

    @PrePersist
    protected fun onCreate() {
        created = Date().time
        updated = created
    }

    @PreUpdate
    protected fun onUpdate() {
        updated = Date().time
    }
}
