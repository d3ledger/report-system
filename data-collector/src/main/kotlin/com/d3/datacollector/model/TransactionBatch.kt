package com.d3.datacollector.model

import com.google.common.collect.ImmutableList
import iroha.protocol.TransactionOuterClass.Transaction
import org.springframework.util.CollectionUtils
import java.util.Spliterator
import java.util.function.Consumer

/**
 * Used to process not only single transaction but batches at once
 */
class TransactionBatch(transactionList: List<Transaction>) : Iterable<Transaction> {

    val transactionList: List<Transaction>

    val batchInitiator: String
        get() = getTxAccountId(transactionList[0])

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

    private fun getTxAccountId(transaction: Transaction): String {
        return transaction.payload.reducedPayload.creatorAccountId
    }
}
