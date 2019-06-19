package com.d3.report.tests.datajpa

import com.d3.report.model.Block
import com.d3.report.model.Transaction
import com.d3.report.model.TransactionBatchEntity
import com.d3.report.model.TransferAsset
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.TransactionBatchRepo
import com.d3.report.repository.TransactionRepo
import com.d3.report.repository.TransferAssetRepo
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import java.util.stream.Collectors
import javax.transaction.Transactional
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@DataJpaTest
class TransactionBatchRepoTest {

    @Autowired
    lateinit var transactionBatchRepo: TransactionBatchRepo
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var txRepo: TransactionRepo
    @Autowired
    lateinit var transferRepo: TransferAssetRepo
    @Value("\${iroha.templates.exchangeBilling}")
    private lateinit var exchangeBillingTemplate: String


    val domain = "oneTest"

    /**
     * @given Some transaction batches in DB
     * @when test that report request works as expected
     * @then Should return only all exchange batches for domain in specified period
     * Parameters to test
     *
     */
    @Test
    @Transactional
    fun testGetTransactionBatchesForAgent() {
        prepareData()
        val page = transactionBatchRepo.getDataBetweenForBillingAccount(
            "$exchangeBillingTemplate$domain",
            1,
            3,
            PageRequest.of(0, 10)
        )
        val result = page.get().collect(Collectors.toList())
        assertEquals(1, result.size)
        val batchTxs = txRepo.getTransactionsByBatchId(result[0].id!!)
        assertEquals(2, batchTxs.size)
    }

    private fun prepareData() {
        prepareDataBlock0()
        prepareDataBlock1()
        prepareDataBlock4()
    }

    private fun prepareDataBlock4() {
        val block = blockRepo.save(Block(4, 4))
        val batch = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )

        var transaction0 = txRepo.save(Transaction(null, block, "mySelf@$domain", 1, false, batch = batch))
        var transaction1 = txRepo.save(Transaction(null, block, "hisSelf@$domain", 1, false, batch = batch))

        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction0
            )
        )
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction0
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction1
            )
        )
        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction1
            )
        )
    }

    private fun prepareDataBlock1() {
        val block0 = blockRepo.save(Block(1, 1))
        val batch0 = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )

        var transaction0 = txRepo.save(Transaction(null, block0, "mySelf@$domain", 1, false, batch = batch0))
        var transaction1 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false, batch = batch0))
        var transaction2 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false))

        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction0
            )
        )
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction0
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction1
            )
        )
        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction1
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "AnyAccount$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction2
            )
        )
    }


    private fun prepareDataBlock0() {
        val block0 = blockRepo.save(Block(0, 0))
        val batch0 = transactionBatchRepo.save(
            TransactionBatchEntity(
                batchType = TransactionBatchEntity.BatchType.EXCHANGE
            )
        )

        var transaction0 = txRepo.save(Transaction(null, block0, "mySelf@$domain", 1, false, batch = batch0))
        var transaction1 = txRepo.save(Transaction(null, block0, "hisSelf@$domain", 1, false, batch = batch0))

        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction0
            )
        )
        transferRepo.save(
            TransferAsset(
                "mySelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction0
            )
        )

        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "destAcc@$domain",
                "assetId@$domain",
                null,
                BigDecimal("10"),
                transaction1
            )
        )
        transferRepo.save(
            TransferAsset(
                "hisSelf@$domain",
                "$exchangeBillingTemplate$domain",
                "assetId@$domain",
                null,
                BigDecimal("0.2"),
                transaction1
            )
        )
    }
}
