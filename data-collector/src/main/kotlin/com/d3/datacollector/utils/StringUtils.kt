/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.utils

fun getDomainFromAccountId(accountId:String) : String = accountId.split("@")[1]
