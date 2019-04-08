package jp.co.soramitsu.d3.datacollector.tasks

import jp.co.soramitsu.d3.datacollector.repository.StateRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledTasks {
    val log = LoggerFactory.getLogger(ScheduledTasks::class.java)


    @Autowired
    lateinit var stateRepo: StateRepository

    @Value("\${iroha.toriiAddress}")
    lateinit var toriiAddress: String

    @Scheduled(fixedDelayString = "\${scheduling.iroha.block.request}", initialDelay = 5000)
    fun processBlock() {
        log.debug("Block downloading job started")

        // stateRepo.findById()


        log.debug("Block downloading job finished")
    }

}