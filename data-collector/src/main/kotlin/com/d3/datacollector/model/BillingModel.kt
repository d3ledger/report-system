/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.model

import com.d3.datacollector.utils.toDcBigDecimal
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "billing")
data class Billing(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val id: Long? = null,
    @JsonIgnore
    val feeDescription: String = "",
    @JsonIgnore
    val domainName: String = "",
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    val billingType: BillingTypeEnum = BillingTypeEnum.TRANSFER,
    @JsonIgnore
    val asset: String = "",
    val destination: String = "",
    @Enumerated(EnumType.STRING)
    val feeType: FeeTypeEnum = FeeTypeEnum.FRACTION,
    @Enumerated(EnumType.STRING)
    val feeNature: FeeNatureEnum = FeeNatureEnum.SUBTRACT,
    @Enumerated(EnumType.STRING)
    val feeComputation: FeeComputationEnum = FeeComputationEnum.FEE,
    val feeAccount: String? = null,
    var feeFraction: BigDecimal = "0.0".toDcBigDecimal(),
    val minAmount: BigDecimal = "0".toDcBigDecimal(),
    val maxAmount: BigDecimal = "-1".toDcBigDecimal(),
    var minFee: BigDecimal = "0".toDcBigDecimal(),
    var maxFee: BigDecimal = "-1".toDcBigDecimal(),
    @JsonIgnore
    var created: Long = 0L,
    @JsonIgnore
    var updated: Long = 0L
) {
    enum class BillingTypeEnum {
        @SerializedName("TRANSFER")
        TRANSFER,
        @SerializedName("CUSTODY")
        CUSTODY,
        @SerializedName("ACCOUNT_CREATION")
        ACCOUNT_CREATION,
        @SerializedName("EXCHANGE")
        EXCHANGE,
        @SerializedName("WITHDRAWAL")
        WITHDRAWAL,
        @SerializedName("NOT_FOUND")
        NOT_FOUND
    }

    enum class FeeTypeEnum {
        @SerializedName("FRACTION")
        FRACTION,
        @SerializedName("FIXED")
        FIXED
    }

    enum class FeeNatureEnum {
        @SerializedName("SUBTRACT")
        SUBTRACT,
        @SerializedName("TRANSFER")
        TRANSFER
    }

    enum class FeeComputationEnum {
        @SerializedName("FEE")
        FEE,
        @SerializedName("TAX")
        TAX
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

    override fun hashCode() =
        (feeDescription + domainName + billingType + asset + destination +
                minAmount.toPlainString() + maxAmount.toPlainString()).hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (this::class != other::class) return false
        val otherBilling = other as Billing
        return this.feeDescription == otherBilling.feeDescription &&
                this.domainName == other.domainName &&
                this.billingType == other.billingType &&
                this.asset == other.asset &&
                this.destination == other.destination &&
                // BigDecimal equals is awful
                this.minAmount.compareTo(other.minAmount) == 0 &&
                this.maxAmount.compareTo(other.maxAmount) == 0
    }
}
