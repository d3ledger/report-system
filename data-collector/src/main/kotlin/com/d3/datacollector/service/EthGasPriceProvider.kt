package com.d3.datacollector.service

import com.d3.datacollector.utils.AdvancedJsonParser.parseStringToJson
import com.d3.datacollector.utils.AdvancedJsonParser.retrieveRate
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class EthGasPriceProvider {
    // tag or tag sequnce to search a value in json
    @Value("\${eth.url}")
    private lateinit var url: String
    @Value("\${eth.key}")
    private lateinit var jsonKey: String

    private var gasPrice: String? = null

    /**
     * Triggers rest execution for retrieving eth gas price using link and tag specified
     */
    fun updateGasPrice() {
        try {
            gasPrice = retrieveRate(parseStringToJson(khttp.get(url).text), jsonKey)
            logger.info("Updated gas price: $gasPrice")
        } catch (e: Exception) {
            logger.error("Couldn't update gas price", e)
        }
    }

    fun getGasPrice(): String? {
        if (gasPrice == null) {
            updateGasPrice()
        }
        return gasPrice
    }

    companion object : KLogging()
}