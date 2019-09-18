/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.*
import javax.persistence.*
import javax.persistence.PreUpdate
import javax.persistence.PrePersist

@Entity
@Table(name = "billing")
data class Billing(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Long? = null,
    @JsonIgnore
    val domainName: String = "",
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    val billingType: BillingTypeEnum = BillingTypeEnum.TRANSFER,
    @JsonIgnore
    val asset: String = "",
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    val feeType: FeeTypeEnum = FeeTypeEnum.FRACTION,
    var feeFraction: BigDecimal = BigDecimal("0.0"),
    var created: Long = 0L,
    var updated: Long = 0L
) {
    enum class BillingTypeEnum {
        @SerializedName("transfer")
        TRANSFER,
        @SerializedName("custody")
        CUSTODY,
        @SerializedName("account_creation")
        ACCOUNT_CREATION,
        @SerializedName("exchange")
        EXCHANGE,
        @SerializedName("withdrawal")
        WITHDRAWAL,
        @SerializedName("not_found")
        NOT_FOUND
    }

    enum class FeeTypeEnum {
        @SerializedName("FRACTION")
        FRACTION,
        @SerializedName("FIXED")
        FIXED
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
