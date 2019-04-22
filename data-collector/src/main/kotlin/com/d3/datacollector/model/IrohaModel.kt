package com.d3.datacollector.model

import java.math.BigDecimal
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "block")
data class Block(
    @Id
    @NotNull
    val blockNumber: Long? = null,
    @NotNull
    val blockCreationTime: Long? = null
)

@Entity
@Table(name = "transaction")
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
    val quorum: Int? = null,
    @NotNull
    var rejected: Boolean = false
)

@MappedSuperclass
open class Command(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @NotNull
    @ManyToOne
    @JoinColumn(name = "transactionId")
    val transaction: Transaction
)

@Entity
@Table(name = "transfer_asset")
class TransferAsset(
    val srcAccountId: String,
    val destAccountId: String,
    val assetId: String,
    val description: String,
    val amount: BigDecimal,
    transaction: Transaction
) : Command(transaction = transaction)

@Entity
@Table(name = "create_account")
class CreateAccount(
    val accountName: String,
    val domainId: String,
    val publicKey: String,
    transaction: Transaction
) : Command(transaction = transaction)

@Entity
@Table(name = "create_asset")
class CreateAsset (
    val assetName: String,
    val domainId: String,
    val decimalPrecision: Int,
    transaction: Transaction
): Command(transaction = transaction)
