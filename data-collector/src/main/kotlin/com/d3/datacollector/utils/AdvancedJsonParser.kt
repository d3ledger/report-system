package com.d3.datacollector.utils

import com.d3.datacollector.service.FinanceService
import com.google.gson.*

object AdvancedJsonParser {
    private val jsonParser = JsonParser()

    fun parseStringToJson(text: String): JsonElement = jsonParser.parse(text)

    private fun retrieveRate(jsonPrimitive: JsonPrimitive): String? {
        return jsonPrimitive.asString
    }

    /**
     * Finds nested attribute value in the [JsonElement] specified
     * @param jsonElement element to examine
     * @param rateAttribute attribute to find a value for
     * @throws [JsonParseException] if there is no such attribute in json
     */
    fun retrieveRate(jsonElement: JsonElement?, rateAttribute: String): String? {
        return when (jsonElement) {
            is JsonPrimitive -> retrieveRate(jsonElement)
            is JsonObject -> retrieveRate(
                jsonElement.asJsonObject,
                rateAttribute
            )
            is JsonArray -> retrieveRate(
                jsonElement.asJsonArray,
                rateAttribute
            )
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
                result =
                    retrieveRate(jsonElement, rateAttribute)
                break
            } catch (e: Exception) {
                FinanceService.logger.warn("Json exception during array parsing", e)
            }
        }
        if (result == null) {
            throw JsonParseException("Couldn't parse json provided")
        }
        return result
    }
}