/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.datacollector.utils

import mu.KLogging
import javax.xml.bind.DatatypeConverter

private val log = KLogging().logger

fun irohaBinaryKeyfromHex(hex: String?): ByteArray? {
    if (hex == null) {
        throw NullPointerException("Key string should be not null")
    }
    val binaryKey = DatatypeConverter.parseHexBinary(hex)
    return binaryKey
}

