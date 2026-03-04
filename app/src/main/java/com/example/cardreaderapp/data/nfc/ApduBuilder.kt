package com.example.cardreaderapp.data.nfc

object ApduBuilder {

    // APDU header values shared with HCECardApp:
    // use standard class 0x00 and INS 0x10, P1=0x00, P2=0x00
    private const val CLA: Byte = 0x00
    private const val INS_SEND_TOKEN: Byte = 0x10
    private const val P1: Byte = 0x00
    private const val P2: Byte = 0x00

    /** AID of the HCE payment app (must match apduservice.xml in HCECardApp). */
    private val AID_PAYMENT = byteArrayOf(
        0xF0.toByte(), 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x01
    )

    /**
     * Build SELECT by AID command (ISO 7816-4).
     * Send this first so the HCE service is selected; otherwise the platform may return 0x6986.
     * Format: CLA 00, INS A4, P1 04 (by name), P2 0C, Lc [AID], [AID]
     */
    fun buildSelectAidCommand(): ByteArray {
        return byteArrayOf(
            0x00,           // CLA
            0xA4.toByte(),  // INS SELECT
            0x04,           // P1 = select by name
            0x0C,           // P2 = first or only
            AID_PAYMENT.size.toByte()
        ) + AID_PAYMENT
    }

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
