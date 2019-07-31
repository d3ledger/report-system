/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.d3.datacollector.utils

import javax.xml.bind.DatatypeConverter

fun irohaBinaryKeyfromHex(hex: String?): ByteArray? {
    if (hex == null) {
        throw NullPointerException("Key string should be not null")
    }
    return DatatypeConverter.parseHexBinary(hex)
}

