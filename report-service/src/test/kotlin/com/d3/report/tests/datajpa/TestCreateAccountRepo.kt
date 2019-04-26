package com.d3.report.tests.datajpa

import com.d3.report.model.Block
import com.d3.report.model.CreateAccount
import com.d3.report.model.Transaction
import com.d3.report.repository.BlockRepository
import com.d3.report.repository.CreateAccountRepo
import com.d3.report.repository.TransactionRepo
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals

@RunWith(SpringRunner::class)
@DataJpaTest
class TestCreateAccountRepo {

    @Autowired
    lateinit var blockRepo: BlockRepository
    @Autowired
    lateinit var transactionRepo: TransactionRepo
    @Autowired
    lateinit var accountRepo: CreateAccountRepo
    @Value("\${iroha.templates.clientsStorage}")
    private lateinit var clientsStorageTemplate: String

    private val domain = "test"

    @Test
    fun testFindAccountsByTemplate() {
        prepareData()
        val accounts = accountRepo.findAccountsByName(clientsStorageTemplate)
        assertEquals(2, accounts.size)
    }

    fun prepareData() {
        var block0 = Block(1, 8)
        block0 = blockRepo.save(block0)
        var transaction0 = Transaction(null, block0, "yourSelf@author", 1, false)
        transaction0 = transactionRepo.save(transaction0)
        val createAccount0 = CreateAccount(clientsStorageTemplate, domain,"Some public key",transaction0)
        accountRepo.save(createAccount0)

        var transaction1 = Transaction(null, block0, "yourSelf@author", 1, false)
        transaction1 = transactionRepo.save(transaction1)
        val createAccount1 = CreateAccount(clientsStorageTemplate, "domainOther","Some public key",transaction1)
        accountRepo.save(createAccount1)

        val createAccount2 = CreateAccount("NotA$clientsStorageTemplate", "domainOther","Some public key",transaction1)
        accountRepo.save(createAccount2)
    }
}
