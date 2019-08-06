package com.d3.datacollector.service

import com.d3.datacollector.model.AssetRate
import com.d3.datacollector.repository.RatesRepository
import com.google.gson.*
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class FinanceService(
    private val ratesRepository: RatesRepository
) {
    @Value("\${iroha.rateAttributeKey}")
    private lateinit var rateAttributeKey: String
    private val jsonParser = JsonParser()

    fun updateRates() {
        val ratesAttributeOptional = ratesRepository.findById(rateAttributeKey)
        if (!ratesAttributeOptional.isPresent || ratesAttributeOptional.get().rate.isNullOrEmpty()) {
            logger.warn("Rates attribute is not set yet, finishing updates")
            return
        }
        val ratesAttribute = ratesAttributeOptional.get().rate!!
        ratesRepository.findAll().filter { !it.link.isNullOrBlank() }.forEach { assetRate ->
            try {
                ratesRepository.save(
                    AssetRate(
                        assetRate.asset,
                        assetRate.link,
                        retrieveRate(jsonParser.parse(khttp.get(assetRate.link!!).text), ratesAttribute)
                    )
                )
            } catch (e: Exception) {
                logger.error("Couldn't update exchange rate for ${assetRate.asset}")
            }
        }
    }

    companion object : KLogging() {
        private fun retrieveRate(jsonPrimitive: JsonPrimitive): String? {
            return jsonPrimitive.asString
        }

        fun retrieveRate(jsonElement: JsonElement, rateAttribute: String): String? {
            return when (jsonElement) {
                is JsonPrimitive -> retrieveRate(jsonElement)
                is JsonObject -> retrieveRate(jsonElement.asJsonObject, rateAttribute)
                is JsonArray -> retrieveRate(jsonElement.asJsonArray, rateAttribute)
                else -> throw JsonParseException("Couldn't parse json provided")
            }
        }

        private fun retrieveRate(jsonObject: JsonObject, rateAttribute: String): String? {
            if (rateAttribute.isEmpty()) {
                throw JsonParseException("Couldn't parse json provided")
            }
            return retrieveRate(
                jsonObject.get(rateAttribute.substringBefore(".")),
                rateAttribute.substringAfter(".", "")
            )
        }

        private fun retrieveRate(jsonArray: JsonArray, rateAttribute: String): String? {
            var result: String? = null
            for (jsonElement in jsonArray) {
                try {
                    result = retrieveRate(jsonElement, rateAttribute)
                    break
                } catch (e: Exception) {
                    logger.warn("Json exception during array parsing", e)
                }
            }
            return result
        }
    }
}