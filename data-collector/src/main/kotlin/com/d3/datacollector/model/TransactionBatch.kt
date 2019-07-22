package com.d3.datacollector.model

import com.google.common.collect.ImmutableList
import iroha.protocol.TransactionOuterClass.Transaction
import org.springframework.util.CollectionUtils
import java.util.*
import java.util.function.Consumer

/**
 * Used to process not only single transaction but batches at once
 */
class TransactionBatch(transactionList: List<Transaction>) : Iterable<Transaction> {

    private val transactionList: List<Transaction>

    init {
        if (CollectionUtils.isEmpty(transactionList)) {
            throw IllegalArgumentException("Batch transaction list cannot be empty")
        }
        this.transactionList = ImmutableList.copyOf(transactionList)
    }

    override fun iterator(): Iterator<Transaction> {
        return transactionList.iterator()
    }

    override fun forEach(action: Consumer<in Transaction>) {
        transactionList.forEach(action)
    }

    override fun spliterator(): Spliterator<Transaction> {
        return transactionList.spliterator()
    }

    fun isBatch() = transactionList.size > 1

    fun hasTransferTo(accountId: String): Boolean {
        return transactionList.stream().anyMatch { transaction ->
            transaction.payload.reducedPayload.commandsList.stream().anyMatch { command ->
                command.hasTransferAsset() && command.transferAsset.destAccountId.startsWith(accountId)
            }
        }
    }
}
