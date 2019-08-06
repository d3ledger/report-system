package com.d3.datacollector.tasks

import com.d3.datacollector.service.FinanceService
import mu.KLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["scheduling.ratesUpdateEnabled"])
class ScheduledFiatRatesTask(
    private val financeService: FinanceService
) {

    @Scheduled(fixedDelayString = "\${scheduling.ratesUpdate}", initialDelay = 5000)
    fun updateFiatRates() {
        try {
            logger.debug("Currencies rate update job started")
            financeService.updateRates()
            logger.debug("Currencies rate update job finished")
        } catch (e: Exception) {
            logger.debug("Error during currencies rate update job", e)
        }
    }

    companion object : KLogging()
}
