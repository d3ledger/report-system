/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.utils

import com.google.gson.Gson
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

fun getNameFromAccountId(accountId: String): String = accountId.split("@")[0]

fun getDomainFromAccountId(accountId: String): String = accountId.split("@")[1]

val gson = Gson()

val mathContext = MathContext(8, RoundingMode.DOWN)

fun String.toDcBigDecimal() = BigDecimal(this, mathContext)