package com.example.cardreaderapp.data.nfc

/**
 * Builds ISO-DEP APDU commands for sending transaction token to receiver.
 * Uses CLA=00, INS=TX (0x74), P1=0, P2=0, data = token bytes.
 */
object ApduBuilder {

    private const val CLA: Byte = 0x00
    private const val INS_SEND_TOKEN: Byte = 0x74 // 'T' for Transaction
    private const val P1: Byte = 0
    private const val P2: Byte = 0

    /**
     * Build APDU command with token payload.
     * Format: CLA INS P1 P2 Lc [data]
     */
    fun buildSendTokenCommand(tokenBytes: ByteArray): ByteArray {
        val lc = tokenBytes.size
        if (lc > 255) throw IllegalArgumentException("Token payload exceeds 255 bytes")
        return byteArrayOf(
            CLA,
            INS_SEND_TOKEN,
            P1,
            P2,
            lc.toByte()
        ) + tokenBytes
    }
}
