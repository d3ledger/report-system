package com.d3.report.tests.datajpa

import com.d3.report.model.Block
import com.d3.report.model.Transaction
import com.d3.report.model.TransferAsset
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.TransactionRepo
import com.d3.report.repository.TransferAssetRepo
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.junit4.SpringRunner
import java.math.BigDecimal
import javax.transaction.Transactional
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@DataJpaTest
class TranferAssetTest {

    @Autowired
    lateinit var em: TestEntityManager
    @Autowired
    lateinit var transferRepo: TransferAssetRepo
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var transactionRepo: TransactionRepo

    @Test
    @Transactional
    fun testTransferSummary() {
        var block1 = Block(1, 129)
        block1 = blockRepo.save(block1)
        var transaction1 = Transaction(null, block1, "mySelf@author", 1, false)
        transaction1 = transactionRepo.save(transaction1)
        val transfer1 = TransferAsset("srcAcc@author", "destAcc@author", "assetId@author",null,  BigDecimal("10"), transaction1)
        transferRepo.save(transfer1)

        var block2 = Block(2, 1299)
        block2 = blockRepo.save(block2)
        var transaction2 = Transaction(null, block2, "mySelf@author", 1, false)
        transaction2 = transactionRepo.save(transaction2)
        val transfer2 = TransferAsset("srcAcc@author", "destAcc@author", "assetId@author",null,  BigDecimal("20"), transaction2)
        transferRepo.save(transfer2)

        var block3 = Block(3, 1398)
        block3 = blockRepo.save(block3)
        var transaction3 = Transaction(null, block3, "mySelf@author", 1, false)
        transaction3 = transactionRepo.save(transaction3)
        val transfer3 = TransferAsset("srcAcc@author", "destAcc@author", "assetId@author",null,  BigDecimal("20"), transaction3)
        transferRepo.save(transfer3)

        var block4 = Block(4, 1499)
        block4 = blockRepo.save(block4)
        var transaction4 = Transaction(null, block4, "mySelf@author", 1, true)
        transaction4 = transactionRepo.save(transaction4)
        val transfer4 = TransferAsset("srcAcc@author", "destAcc@author", "assetId@author",null,  BigDecimal("20"), transaction4)
        transferRepo.save(transfer4)

        val dbData = transferRepo.getDataBetween(130L, 13000)
        assertEquals(2, dbData.size)

    }
}
