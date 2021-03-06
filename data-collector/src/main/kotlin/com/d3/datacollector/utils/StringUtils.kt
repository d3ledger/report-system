/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.utils

fun getNameFromAccountId(accountId: String): String = accountId.split("@")[0]

fun getDomainFromAccountId(accountId: String): String = accountId.split("@")[1]
