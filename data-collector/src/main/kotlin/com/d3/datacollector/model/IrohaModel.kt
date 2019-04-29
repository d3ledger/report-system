package com.d3.datacollector.model

import com.fasterxml.jackson.annotation.JsonIgnore
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
    var rejected: Boolean = false,
    @JsonIgnore
    @OneToMany(mappedBy = "transaction", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val commands:List<Command> = ArrayList()
)

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
open class Command(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactionId")
    val transaction: Transaction
)

@Entity
@Table(name = "transfer_asset")
class TransferAsset(
    val srcAccountId: String? = null,
    val destAccountId: String? = null,
    val assetId: String? = null,
    val description: String? = null,
    val amount: BigDecimal? = null,
    transaction: Transaction = Transaction()
) : Command(transaction = transaction)

@Entity
@Table(name = "create_account")
class CreateAccount(
    val accountName: String? = null,
    val domainId: String? = null,
    val publicKey: String? = null,
    transaction: Transaction = Transaction()
) : Command(transaction = transaction)

@Entity
@Table(name = "create_asset")
class CreateAsset(
    val assetName: String,
    val domainId: String,
    val decimalPrecision: Int,
    transaction: Transaction
) : Command(transaction = transaction)

@Entity
@Table(name = "set_account_detail")
class SetAccountDetail(
    val account_id: String? = null,
    val detailKey: String? = null,
    val detailValue: String? = null,
    transaction: Transaction
) : Command(transaction = transaction)
