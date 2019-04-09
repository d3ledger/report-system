package jp.co.soramitsu.d3.datacollector.service

import jp.co.soramitsu.d3.datacollector.model.State
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class DbService {


    @Autowired
    lateinit var stateRepo: StateRepository

    fun updatePropertiesInDatabase(
        lastBlockState: State,
        lastRequest: State
    ) {
        var newLastBlock = lastBlockState.value.toLong()
        newLastBlock++
        lastBlockState.value = newLastBlock.toString()
        stateRepo.save(lastBlockState)
        var newQueryNumber = lastRequest.value.toLong()
        newQueryNumber++
        lastRequest.value = newQueryNumber.toString()
        stateRepo.save(lastRequest)
    }
}