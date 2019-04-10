package jp.co.soramitsu.d3.datacollector.service

import jp.co.soramitsu.d3.datacollector.model.Billing
import jp.co.soramitsu.d3.datacollector.model.State
import jp.co.soramitsu.d3.datacollector.repository.BillingRepository
import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class DbService {


    @Autowired
    lateinit var stateRepo: StateRepository
    @Autowired
    lateinit var billingRepo: BillingRepository

    @Transactional
    fun updateBillingInDb(
        billing: Billing
    ) {
        val found =
            billingRepo.selectByAccountIdBillingTypeAndAsset(billing.accountId, billing.asset, billing.billingType)
        if (found.isPresent) {
            val updated = Billing(id = found.get().id, feeFraction = billing.feeFraction,created = found.get().created)
            billingRepo.save(updated)
        } else {
            billingRepo.save(billing)

        }
    }

    @Transactional
    fun updateStateInDb(
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