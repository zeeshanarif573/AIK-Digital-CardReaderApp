package com.example.cardreaderapp.data.nfc

import com.example.cardreaderapp.domain.model.ApduResponse

/**
 * Parses response APDU from receiver device.
 * Expected format: SW1 SW2 (status bytes).
 * SW1=0x90, SW2=0x00 = success; otherwise error.
 */
object ApduParser {

    private const val SW1_SUCCESS: Byte = 0x90.toByte()
    private const val SW2_SUCCESS: Byte = 0x00

    /**
     * Parse raw response APDU into status and optional data.
     */
    fun parseResponse(response: ByteArray): ApduResponse {
        if (response.size < 2) {
            return ApduResponse(
                statusCode = -1,
                statusMessage = "Response too short"
            )
        }
        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        val data = if (response.size > 2) {
            response.copyOfRange(0, response.size - 2)
        } else {
            null
        }
        return if (sw1 == SW1_SUCCESS && sw2 == SW2_SUCCESS) {
            ApduResponse(
                statusCode = 0x9000,
                statusMessage = "OK",
                data = data
            )
        } else {
            val code = ((sw1.toInt() and 0xFF) shl 8) or (sw2.toInt() and 0xFF)
            val hint = when (code) {
                0x6986 -> " (HCE app not selected—ensure SELECT AID is sent first)"
                0x6A82 -> " (AID not found—is HCECardApp installed and in foreground on the other device?)"
                else -> ""
            }
            ApduResponse(
                statusCode = code,
                statusMessage = "SW1=${sw1.toInt() and 0xFF}, SW2=${sw2.toInt() and 0xFF}$hint",
                data = data
            )
        }
    }
}
