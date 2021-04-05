/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package it.keybiz.lbsapp.corporate.connection

import it.keybiz.lbsapp.corporate.utilities.Utils
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Object whose duty is to handle en- and decrypting of the incoming and outgoing messages from and
 * to HL servers.
 *
 * Algorithm used: AES_256 with CBC
 *
 * Padding: PKCS7Padding
 *
 * Important note: due to the different definition of [Byte] between C# (0..255) and Java/Kotlin
 * (-128..127) the byte representations between Android client and server will be different.
 */
object Crypto {

    private val K_0 = "5"
    private val K_1 = "L"
    private val K_2 = "y"
    private val K_3 = "6"
    private val K_4 = "w"
    private val K_5 = "w"
    private val K_6 = "E"
    private val K_7 = "k"
    private val K_8 = "6"
    private val K_9 = "f"
    private val K_10 = "m"
    private val K_11 = "7"
    private val K_12 = "h"
    private val K_13 = "B"
    private val K_14 = "u"
    private val K_15 = "L"
    private val K_16 = "W"
    private val K_17 = "u"
    private val K_18 = "S"
    private val K_19 = "9"
    private val K_20 = "L"
    private val K_21 = "M"
    private val K_22 = "F"
    private val K_23 = "D"
    private val K_24 = "h"
    private val K_25 = "w"
    private val K_26 = "3"
    private val K_27 = "q"
    private val K_28 = "e"
    private val K_29 = "T"
    private val K_30 = "P"
    private val K_31 = "F"

    private val IV_0 = "2"
    private val IV_1 = "v"
    private val IV_2 = "Q"
    private val IV_3 = "4"
    private val IV_4 = "="
    private val IV_5 = "P"
    private val IV_6 = "!"
    private val IV_7 = "?"
    private val IV_8 = "K"
    private val IV_9 = "?"
    private val IV_10 = "a"
    private val IV_11 = "w"
    private val IV_12 = "W"
    private val IV_13 = "f"
    private val IV_14 = "f"
    private val IV_15 = "6"

    private val AES = "AES_256"
    private val AES_LEGACY = "AES"
    private val BLOCKING = "CBC"
    private val PADDING = "PKCS7Padding"

    private val TRANSFORMATION = "${if (Utils.hasOreo()) AES else AES_LEGACY}/$BLOCKING/$PADDING"


    @JvmStatic fun encryptData(data: String?): ByteArray? {
        return if (!data.isNullOrBlank()) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val keySpec = SecretKeySpec(retrieveKey(), if (Utils.hasOreo()) AES else AES_LEGACY)
            val ivSpec = IvParameterSpec(retrieveIV())
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

            val encryptedData = cipher.doFinal(data.toByteArray())

            encryptedData
        }
        else null
    }

    @JvmStatic fun decryptData(data: ByteArray?): String? {
        return if (data?.isNotEmpty() == true) {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val keySpec = SecretKeySpec(retrieveKey(), if (Utils.hasOreo()) AES else AES_LEGACY)
            val ivSpec = IvParameterSpec(retrieveIV())
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decryptedBytes = cipher.doFinal(data)

            String(decryptedBytes)
        }
        else null
    }


    private fun retrieveKey(): ByteArray {
        val newArr = arrayOfNulls<String>(32)

        newArr[0] = K_27
        newArr[1] = K_19
        newArr[2] = K_28
        newArr[3] = K_10
        newArr[4] = K_24
        newArr[5] = K_21
        newArr[6] = K_15
        newArr[7] = K_20
        newArr[8] = K_25
        newArr[9] = K_11
        newArr[10] = K_31
        newArr[11] = K_7
        newArr[12] = K_14
        newArr[13] = K_4
        newArr[14] = K_1
        newArr[15] = K_16
        newArr[16] = K_12
        newArr[17] = K_2
        newArr[18] = K_26
        newArr[19] = K_23
        newArr[20] = K_6
        newArr[21] = K_29
        newArr[22] = K_18
        newArr[23] = K_22
        newArr[24] = K_30
        newArr[25] = K_5
        newArr[26] = K_3
        newArr[27] = K_0
        newArr[28] = K_17
        newArr[29] = K_9
        newArr[30] = K_13
        newArr[31] = K_8

        val builder = StringBuilder()
        for (it in newArr)
            builder.append(it)

        return builder.toString().toByteArray()
    }

    private fun retrieveIV(): ByteArray {
        val newArr = arrayOfNulls<String>(16)

        newArr[0] = IV_3
        newArr[1] = IV_6
        newArr[2] = IV_12
        newArr[3] = IV_13
        newArr[4] = IV_14
        newArr[5] = IV_4
        newArr[6] = IV_10
        newArr[7] = IV_11
        newArr[8] = IV_5
        newArr[9] = IV_2
        newArr[10] = IV_7
        newArr[11] = IV_9
        newArr[12] = IV_15
        newArr[13] = IV_0
        newArr[14] = IV_1
        newArr[15] = IV_8

        val builder = StringBuilder()
        for (it in newArr)
            builder.append(it)

        return builder.toString().toByteArray()
    }


    /**
     * Extension function that uses [StandardCharsets.UTF_8] as default [Charsets].
     */
    private fun String.toByteArray(): ByteArray {
        return this.toByteArray(StandardCharsets.UTF_8)
    }


}