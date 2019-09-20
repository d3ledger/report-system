package com.d3.datacollector.tasks

import com.d3.datacollector.service.EthGasPriceProvider
import mu.KLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["scheduling.gasPriceUpdateEnabled"])
class ScheduledGasPriceTask(
    private val ethGasPriceProvider: EthGasPriceProvider
) {

    @Scheduled(fixedDelayString = "\${scheduling.gasPriceUpdate}", initialDelay = 5000)
    fun updateGasPrice() {
        try {
            logger.debug("Gas price update job started")
            ethGasPriceProvider.updateGasPrice()
            logger.debug("Gas price rate update job finished")
        } catch (e: Exception) {
            logger.error("Error during gas price update job", e)
        }
    }

    companion object : KLogging()
}
