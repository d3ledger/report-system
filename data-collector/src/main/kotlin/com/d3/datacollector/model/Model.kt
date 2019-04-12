package com.d3.datacollector.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.math.BigDecimal
import java.util.*
import javax.persistence.*
import javax.persistence.PreUpdate
import javax.persistence.PrePersist


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
    val asset:String = "",
    var feeFraction: BigDecimal = BigDecimal("0.015"),
    var created: Date? = null,
    @JsonIgnore
    var updated: Date? = null
) {
    enum class BillingTypeEnum {
        TRANSFER,
        CUSTODY,
        ACCOUNT_CREATION,
        EXCHANGE,
        WITHDRAWAL,
        NOT_FOUND
    }

    @PrePersist
    protected fun onCreate() {
        created = Date()
    }

    @PreUpdate
    protected fun onUpdate() {
        updated = Date()
    }
}
