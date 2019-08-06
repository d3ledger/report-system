package com.d3.datacollector.model

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "rates")
data class AssetRate(
    @Id
    val asset: String,
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