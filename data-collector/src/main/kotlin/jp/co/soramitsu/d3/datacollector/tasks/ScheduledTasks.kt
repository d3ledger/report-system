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
    fun processBlock() {
        log.debug("Block downloading job started")

        var (lastBlock, lastRequest) = service.getMetadata()

        val response = service.irohaBlockQuery(lastRequest, lastBlock)

        if (response.hasBlockResponse()) {
            if (response.hasBlockResponse()) {

            } else {
                log.error("No block or error response catched from Iroha: $response")
            }
        } else if (response.hasErrorResponse()) {
            if (response.errorResponse.errorCode == 3) {
                log.debug("Highest block riched. Finishing blocks downloading job execution")
            } else {
                val error = response.errorResponse
                log.error("Blocks querying job error: errorCode: ${error.errorCode}, message: ${error.message}")
            }
        }
        log.debug("Block downloading job finished")
    }


}