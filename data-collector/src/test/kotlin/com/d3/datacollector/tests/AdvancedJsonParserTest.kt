/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.tests

import com.d3.datacollector.service.FinanceService
import com.d3.datacollector.utils.AdvancedJsonParser.retrieveRate
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.junit.Test
import kotlin.test.assertEquals

class AdvancedJsonParserTest {

    private val json = JsonParser().parse(
        "{\n" +
                "  \"quoteResponse\": {\n" +
                "    \"result\": [\n" +
                "      {\n" +
                "        \"fullExchangeName\": \"CCY\",\n" +
                "        \"symbol\": \"JPY=X\",\n" +
                "        \"fiftyTwoWeekLowChangePercent\": {\n" +
                "          \"raw\": 0.008568714,\n" +
                "          \"fmt\": \"0.86%\"\n" +
                "        },\n" +
                "        \"gmtOffSetMilliseconds\": 3600000,\n" +
                "        \"regularMarketOpen\": {\n" +
                "          \"raw\": 105.811,\n" +
                "          \"fmt\": \"105.8110\"\n" +
                "        },\n" +
                "        \"language\": \"en-US\",\n" +
                "        \"regularMarketTime\": {\n" +
                "          \"raw\": 1565084291,\n" +
                "          \"fmt\": \"10:38AM BST\"\n" +
                "        },\n" +
                "        \"regularMarketChangePercent\": {\n" +
                "          \"raw\": 0.5604354,\n" +
                "          \"fmt\": \"0.5604%\"\n" +
                "        },\n" +
                "        \"uuid\": \"2468a80f-e3c8-3e12-b90d-51445a7c3bae\",\n" +
                "        \"quoteType\": \"CURRENCY\",\n" +
                "        \"regularMarketDayRange\": {\n" +
                "          \"raw\": \"105.5 - 107.09\",\n" +
                "          \"fmt\": \"105.50 - 107.09\"\n" +
                "        },\n" +
                "        \"fiftyTwoWeekLowChange\": {\n" +
                "          \"raw\": 0.9039993,\n" +
                "          \"fmt\": \"0.90\"\n" +
                "        },\n" +
                "        \"fiftyTwoWeekHighChangePercent\": {\n" +
                "          \"raw\": -0.07079671,\n" +
                "          \"fmt\": \"-7.08%\"\n" +
                "        },\n" +
                "        \"regularMarketDayHigh\": {\n" +
                "          \"raw\": 107.09,\n" +
                "          \"fmt\": \"107.0900\"\n" +
                "        },\n" +
                "        \"tradeable\": false,\n" +
                "        \"currency\": \"JPY\",\n" +
                "        \"fiftyTwoWeekHigh\": {\n" +
                "          \"raw\": 114.511,\n" +
                "          \"fmt\": \"114.5110\"\n" +
                "        },\n" +
                "        \"regularMarketPreviousClose\": {\n" +
                "          \"raw\": 105.811,\n" +
                "          \"fmt\": \"105.8110\"\n" +
                "        },\n" +
                "        \"exchangeTimezoneName\": \"Europe/London\",\n" +
                "        \"fiftyTwoWeekHighChange\": {\n" +
                "          \"raw\": -8.107002,\n" +
                "          \"fmt\": \"-8.11\"\n" +
                "        },\n" +
                "        \"fiftyTwoWeekRange\": {\n" +
                "          \"raw\": \"105.5 - 114.511\",\n" +
                "          \"fmt\": \"105.50 - 114.51\"\n" +
                "        },\n" +
                "        \"regularMarketChange\": {\n" +
                "          \"raw\": 0.5930023,\n" +
                "          \"fmt\": \"0.5930\"\n" +
                "        },\n" +
                "        \"exchangeDataDelayedBy\": 0,\n" +
                "        \"exchangeTimezoneShortName\": \"BST\",\n" +
                "        \"marketState\": \"REGULAR\",\n" +
                "        \"fiftyTwoWeekLow\": {\n" +
                "          \"raw\": 105.5,\n" +
                "          \"fmt\": \"105.5000\"\n" +
                "        },\n" +
                "        \"regularMarketPrice\": {\n" +
                "          \"raw\": 106.404,\n" +
                "          \"fmt\": \"106.4040\"\n" +
                "        },\n" +
                "        \"market\": \"ccy_market\",\n" +
                "        \"regularMarketVolume\": {\n" +
                "          \"raw\": 0,\n" +
                "          \"fmt\": \"0\",\n" +
                "          \"longFmt\": \"0\"\n" +
                "        },\n" +
                "        \"quoteSourceName\": \"Delayed Quote\",\n" +
                "        \"messageBoardId\": \"finmb_JPY_X\",\n" +
                "        \"priceHint\": 4,\n" +
                "        \"regularMarketDayLow\": {\n" +
                "          \"raw\": 105.5,\n" +
                "          \"fmt\": \"105.5000\"\n" +
                "        },\n" +
                "        \"exchange\": \"CCY\",\n" +
                "        \"sourceInterval\": 15,\n" +
                "        \"shortName\": \"USD/JPY\",\n" +
                "        \"region\": \"US\",\n" +
                "        \"triggerable\": false\n" +
                "      }\n" +
                "    ],\n" +
                "    \"error\": null\n" +
                "  }\n" +
                "}"
    )

    /**
     * @given properly formatted json object
     * @when [FinanceService] finds a value for quoteResponse.result.regularMarketPrice.raw nested attribute
     * @then the value is correct being equal to 106.404
     */
    @Test
    fun testJsonParsing() {
        assertEquals(
            "106.404",
            retrieveRate(json, "quoteResponse.result.regularMarketPrice.raw")
        )
    }

    /**
     * @given properly formatted json object
     * @when [FinanceService] finds a value for unknown quoteResponse.result.regularMarketPrice.some nested attribute
     * @then the value is correct being equal to 106.404
     */
    @Test(expected = JsonParseException::class)
    fun testJsonBadParsing() {
        retrieveRate(json, "quoteResponse.result.regularMarketPrice.some")
    }
}
