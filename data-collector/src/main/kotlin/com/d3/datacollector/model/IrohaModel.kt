package com.d3.datacollector.model

import java.math.BigDecimal
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "block", schema = "iroha")
data class Block(
    @Id
    val blockNumber: Long? = null,
    @NotNull
    val timestamp: Long? = null
)

@Entity
@Table(name = "transaction", schema = "iroha")
data class Transaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @NotNull
    @ManyToOne
    @JoinColumn(name = "blockNumber")
    val block: Block? = null,
    @NotNull
    val creatorId: String? = null,
    @NotNull
    val quorum: Int? = null
)

@MappedSuperclass
open class Command (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @NotNull
    @ManyToOne
    @JoinColumn(name = "transactionId")
    val transaction: Transaction? = null,
    @NotNull
    @Enumerated(EnumType.STRING)
    val type:CommandType? = null
) {
    enum class CommandType {
        TransferAsset,
        CreateAccount
    }
}

@Entity
@Table(name = "transfer_asset", schema = "iroha")
data class TransferAsset(
    val srcAccountId: String,
    val destAccountId: String,
    val assetId: String,
    val description: String,
    val amount: BigDecimal
) : Command()
