/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.report.model

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
    val blockCreationTime: Long = 0
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
    @OneToMany(mappedBy = "transaction", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    val commands: MutableList<Command> = ArrayList(),
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batchId")
    val batch: TransactionBatchEntity? = null
)

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
open class Command(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @NotNull
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transactionId")
    val transaction: Transaction? = null
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
class CreateAsset(
    val assetName: String = "empty Asset name",
    val domainId: String = "empty domain Id",
    val decimalPrecision: Int = 8,
    transaction: Transaction = Transaction()
) : Command(transaction = transaction)

@Entity
@Table(name = "set_account_detail")
class SetAccountDetail(
    val accountId: String? = null,
    val detailKey: String? = null,
    val detailValue: String? = null,
    transaction: Transaction = Transaction()
) : Command(transaction = transaction)

@Entity
@Table(name = "set_account_quorum")
class SetAccountQuorum(
    val accountId: String? = null,
    val quorum: Int = 1,
    transaction: Transaction = Transaction()
) : Command(transaction = transaction)

@Entity
@Table(name = "add_signatory")
class AddSignatory(
    val accountId: String? = null,
    val publicKey: String? = null,
    transaction: Transaction = Transaction()
) : Command(transaction = transaction)


@Entity
@Table(name = "transaction_batch")
data class TransactionBatchEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @OneToMany(mappedBy = "batch", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var transactions: List<Transaction> = ArrayList(),
    @Enumerated(EnumType.STRING)
    val batchType: BatchType = BatchType.UNDEFINED
) {
    enum class BatchType {
        UNDEFINED,
        EXCHANGE
    }
}
