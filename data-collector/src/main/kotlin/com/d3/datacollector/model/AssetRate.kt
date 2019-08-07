package com.d3.datacollector.model

import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

/**
 * Asset rates table entity representation
 * @param asset Iroha asset id
 * @param link http url to execute for the asset
 * @param rate current exchange rate of the asset
 * @param updated last time in millis the rate was updated
 */
@Entity
@Table(name = "rates")
data class AssetRate(
    @Id
    @NotNull
    val asset: String? = null,
    val link: String? = null,
    val rate: String? = null,
    var updated: Long = 0L
) {
    @PrePersist
    @PreUpdate
    protected fun onUpdate() {
        updated = Date().time
    }
}
