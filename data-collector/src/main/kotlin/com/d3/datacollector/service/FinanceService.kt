package com.d3.datacollector.service

import com.d3.datacollector.model.AssetRate
import com.d3.datacollector.repository.RatesRepository
import com.d3.datacollector.utils.AdvancedJsonParser.parseStringToJson
import com.d3.datacollector.utils.AdvancedJsonParser.retrieveRate
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service for updating assets exchange rates using REST
 */
@Service
class FinanceService(
    private val ratesRepository: RatesRepository
) {
    // tag or tag sequnce to search a value in json
    @Value("\${iroha.rateAttributeKey}")
    private lateinit var rateAttributeKey: String

    /**
     * Triggers rest execution for all the assets specified and updates repository correspondingly
     */
    fun updateRates() {
        val ratesAttributeOptional = ratesRepository.findById(rateAttributeKey)
        if (!ratesAttributeOptional.isPresent || ratesAttributeOptional.get().rate.isNullOrEmpty()) {
            logger.warn("Rates attribute is not set yet, finishing updates")
            return
        }
        val ratesAttribute = ratesAttributeOptional.get().rate!!
        // for all assets with relevant url
        ratesRepository.findAll().filter { !it.link.isNullOrBlank() }.forEach { assetRate ->
            try {
                val rate = retrieveRate(parseStringToJson(khttp.get(assetRate.link!!).text), ratesAttribute)
                ratesRepository.save(
                    AssetRate(
                        assetRate.asset,
                        assetRate.link,
                        rate
                    )
                )
                logger.info("Updated rate: ${assetRate.asset} $rate")
            } catch (e: Exception) {
                logger.error("Couldn't update exchange rate for ${assetRate.asset}")
            }
        }
    }

    companion object : KLogging()
}
