package jp.co.soramitsu.d3.datacollector.tasks

import jp.co.soramitsu.d3.datacollector.service.BlockTaskService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ScheduledTasks {
    val log = LoggerFactory.getLogger(ScheduledTasks::class.java)

    @Autowired
    lateinit var service: BlockTaskService

    @Scheduled(fixedDelayString = "\${scheduling.iroha.block.request}", initialDelay = 5000)
    fun getBlockTask() {
        log.debug("Block downloading job started")
        service.processBlockTask()
        log.debug("Block downloading job finished")
    }
}