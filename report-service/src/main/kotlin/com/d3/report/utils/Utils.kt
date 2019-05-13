/*
 *
 *  Copyright D3 Ledger, Inc. All Rights Reserved.
 *  SPDX-License-Identifier: Apache-2.0
 * /
 */

package com.d3.report.utils

import com.d3.report.model.CreateAccount

fun getAccountId(account: CreateAccount) =
    "${account.accountName}@${account.domainId}"
