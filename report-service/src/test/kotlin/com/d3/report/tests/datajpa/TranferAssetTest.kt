/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
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
class TranferAssetTest {

    @Autowired
    lateinit var transferRepo: TransferAssetRepo
    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var transactionRepo: TransactionRepo
    @Value("\${iroha.templates.transferBilling}")
    private lateinit var transferBillingTemplate: String

    private val domain = "author"

    /**
     * @given Some transactions and transfers in DB
     * @when request all transfers for account in a period
     * @then Should return all transfers where account is source account and transfer fee for transfer exists.
     */
    @Test
    @Transactional
    fun testAllTransfersForCustomer() {
        val block1 = blockRepo.save(Block(1, 1))
        val transaction1 = transactionRepo.save(Transaction(null, block1, "mySelf@$domain", 1, false))
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("1"), transaction1))

        val block2 = blockRepo.save(Block(2, 2))
        val transaction2 = transactionRepo.save(Transaction(null, block2, "mySelf@$domain", 1, false))
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("10"), transaction2))
        transferRepo.save(TransferAsset("srcAcc@$domain", "${transferBillingTemplate}$domain", "assetId@$domain", null, BigDecimal("1"), transaction2))

        val transaction21 = transactionRepo.save(Transaction(null, block2, "mySelf@$domain", 1, false))
        transferRepo.save(TransferAsset("srcTwoAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("10"), transaction21))
        transferRepo.save(TransferAsset("srcTwoAcc@$domain", "${transferBillingTemplate}$domain", "assetId@$domain", null, BigDecimal("1"), transaction21))
      
        var block3 = blockRepo.save(Block(3, 5))
        var transaction3 = transactionRepo.save(Transaction(null, block3, "mySelf@$domain", 1, false))
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("1"), transaction3))
        transferRepo.save(TransferAsset("srcAcc@$domain", "${transferBillingTemplate}$domain", "assetId@$domain", null, BigDecimal("1"), transaction3))

        val page = transferRepo.getDataBetween("srcAcc@$domain", "${transferBillingTemplate}$domain", 2, 4, PageRequest.of(0, 5))
        assertEquals(2,page.numberOfElements)
    }

    /**
     * @given Some transactions in DB
     * @when request all transfer data for account
     * @then Should return all transfers where account is source or destination account.
     */
    @Test
    @Transactional
    fun testGetAllTransferDataForAccount() {
        var block2 = Block(2, 1299)
        block2 = blockRepo.save(block2)
        var transaction2 = Transaction(null, block2, "mySelf@$domain", 1, false)
        transaction2 = transactionRepo.save(transaction2)
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("20"), transaction2))

        var block1 = Block(1, 129)
        block1 = blockRepo.save(block1)
        var transaction1 = Transaction(null, block1, "mySelf@$domain", 1, false)
        transaction1 = transactionRepo.save(transaction1)
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("1"), transaction1))
        transferRepo.save(TransferAsset("srcNotAcc@$domain", "srcAcc@$domain", "assetId@$domain", null, BigDecimal("2"), transaction1))
        transferRepo.save(TransferAsset("srcAcc@domainTwo", "destAcc@$domain", "assetId@$domain", null, BigDecimal("12"), transaction1))
        transferRepo.save(TransferAsset("otherAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("14"), transaction1))

        val page = transferRepo.getAllDataForAccount("srcAcc@$domain", PageRequest.of(0, 5))
        assertEquals(3,page.numberOfElements)
        /*
        Check order of transactions
         */
        assertEquals(BigDecimal("20"), page.get().collect(Collectors.toList())[2].amount)
    }

    /**
     * @given Some transactions in DB
     * @when request all transfer data for account
     * @then Should return all transfers up to specified time parameter where account is source or destination account.
     */
    @Test
    @Transactional
    fun testGetBorderedTransferDataForAccount() {
        var block2 = Block(2, 1199)
        block2 = blockRepo.save(block2)
        var transaction2 = Transaction(null, block2, "mySelf@$domain", 1, false)
        transaction2 = transactionRepo.save(transaction2)
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("3"), transaction2))


        var block3 = Block(3, 1299)
        block3 = blockRepo.save(block3)
        var transaction3 = Transaction(null, block3, "mySelf@$domain", 1, false)
        transaction3 = transactionRepo.save(transaction3)
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("20"), transaction3))

        var block1 = Block(1, 129)
        block1 = blockRepo.save(block1)
        var transaction1 = Transaction(null, block1, "mySelf@$domain", 1, false)
        transaction1 = transactionRepo.save(transaction1)
        transferRepo.save(TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("1"), transaction1))
        transferRepo.save(TransferAsset("srcNotAcc@$domain", "srcAcc@$domain", "assetId@$domain", null, BigDecimal("2"), transaction1))
        transferRepo.save(TransferAsset("srcAcc@domainTwo", "destAcc@$domain", "assetId@$domain", null, BigDecimal("12"), transaction1))
        transferRepo.save(TransferAsset("otherAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("14"), transaction1))

        val page = transferRepo.getTimedDataForAccount("srcAcc@$domain", 1200, PageRequest.of(0, 5))
        assertEquals(3,page.numberOfElements)
        /*
        Check order of transactions
         */
        assertEquals(BigDecimal("3"), page.get().collect(Collectors.toList())[2].amount)
    }

    @Test
    @Transactional
    fun testTransferExclusion() {
        prepareData()

        val dbData = transferRepo.getDataBetween(transferBillingTemplate,130, 13000, PageRequest.of(0, 5))
        assertEquals(0, dbData.get().collect(Collectors.toList()).size)
    }

    @Test
    @Transactional
    fun testTransferOneTransactionWithCorrectTransferAndFee() {
        prepareDataContainsOneTransactionWithCorrectTransferAndFee()

        val dbData = transferRepo.getDataBetween("${transferBillingTemplate}$domain",130, 13000, PageRequest.of(0, 5))
        assertEquals(2, dbData.get().collect(Collectors.toList()).size)

    }

    private fun prepareData() {
        var block1 = Block(1, 129)
        block1 = blockRepo.save(block1)
        var transaction1 = Transaction(null, block1, "mySelf@$domain", 1, false)
        transaction1 = transactionRepo.save(transaction1)
        val transfer1 =
            TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("10"), transaction1)
        transferRepo.save(transfer1)

        var block2 = Block(2, 1299)
        block2 = blockRepo.save(block2)
        var transaction2 = Transaction(null, block2, "mySelf@$domain", 1, false)
        transaction2 = transactionRepo.save(transaction2)
        val transfer2 =
            TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("20"), transaction2)
        transferRepo.save(transfer2)

        var block3 = Block(3, 1398)
        block3 = blockRepo.save(block3)
        var transaction3 = Transaction(null, block3, "mySelf@$domain", 1, false)
        transaction3 = transactionRepo.save(transaction3)
        val transfer3 =
            TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("20"), transaction3)
        transferRepo.save(transfer3)

        var block4 = Block(4, 1499)
        block4 = blockRepo.save(block4)
        var transaction4 = Transaction(null, block4, "mySelf@$domain", 1, true)
        transaction4 = transactionRepo.save(transaction4)
        val transfer4 =
            TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("20"), transaction4)
        transferRepo.save(transfer4)
    }

    private fun prepareDataContainsOneTransactionWithCorrectTransferAndFee() {
        var block3 = Block(3, 1398)
        block3 = blockRepo.save(block3)
        var transaction3 = Transaction(null, block3, "mySelf@$domain", 1, false)
        transaction3 = transactionRepo.save(transaction3)
        val transfer3 =
            TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("20"), transaction3)
        transferRepo.save(transfer3)

        var block4 = Block(4, 1499)
        block4 = blockRepo.save(block4)
        var transaction4 = Transaction(null, block4, "mySelf@$domain", 1, true)
        transaction4 = transactionRepo.save(transaction4)
        val transfer4 =
            TransferAsset("srcAcc@$domain", "destAcc@$domain", "assetId@$domain", null, BigDecimal("20"), transaction4)
        transferRepo.save(transfer4)

        val transfer5 =
            TransferAsset("srcAcc@$domain", "$transferBillingTemplate$domain", "assetId@$domain", null, BigDecimal("0.2"), transaction3)
        transferRepo.save(transfer5)
    }
}
