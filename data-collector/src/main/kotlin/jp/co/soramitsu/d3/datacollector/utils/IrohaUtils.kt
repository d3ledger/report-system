package jp.co.soramitsu.d3.datacollector.utils

import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.d3.datacollector.exceptions.IrohaKeyException
import mu.KLogging
import java.security.PrivateKey
import java.security.PublicKey
import javax.xml.bind.DatatypeConverter

private val log = KLogging().logger

fun irohaPublicKeyFromHex(hex: String?): PublicKey {
    try {
        val binaryKey = irohaBinaryKeyfromHex(hex)
        return Ed25519Sha3.publicKeyFromBytes(binaryKey)
    } catch (e: Exception) {
        log.error("Error parsing publicKey", e)
        throw IrohaKeyException("${e.javaClass}:${e.message}")
    }
}

fun irohaPrivateKeyFromHex(hex: String?): PrivateKey {
    try {
        val binaryKey = irohaBinaryKeyfromHex(hex)
        return Ed25519Sha3.privateKeyFromBytes(binaryKey)
    } catch (e: Exception) {
        log.error("Error parsing privateKey", e)
        throw IrohaKeyException("${e.javaClass}:${e.message}")
    }
}

fun irohaBinaryKeyfromHex(hex: String?): ByteArray? {
    if (hex == null) {
        throw NullPointerException("Key string should be not null")
    }
    val binaryKey = DatatypeConverter.parseHexBinary(hex)
    return binaryKey
}

