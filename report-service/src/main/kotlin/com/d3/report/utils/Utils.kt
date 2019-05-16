/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.report.utils

import com.d3.report.model.CreateAccount

fun getAccountId(account: CreateAccount) : String {
    if(account.accountName == null || account.domainId == null) {
        throw IllegalArgumentException("accountName and domainId should be not null")
    }
    return "${account.accountName}@${account.domainId}"
}
