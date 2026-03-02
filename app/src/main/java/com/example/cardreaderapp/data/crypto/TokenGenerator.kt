package com.example.cardreaderapp.data.crypto

import com.example.cardreaderapp.domain.model.TransactionToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.UUID

/**
 * Generates signed transaction tokens: nonce, timestamp, deviceId, amount, txnType,
 * canonical payload string, and Base64 signature.
 */
class TokenGenerator(
    private val signer: KeystoreSigner,
    private val deviceIdProvider: () -> String
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Canonical payload for signing: deviceId|amount|nonce|timestamp|txnType
     */
    fun buildCanonicalPayload(
        deviceId: String,
        amount: String,
        nonce: String,
        timestamp: Long,
        txnType: String
    ): String = listOf(deviceId, amount, nonce, timestamp.toString(), txnType).joinToString("|")

    /**
     * Generate cryptographically secure random nonce (hex string).
     */
    fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Build and sign token. Returns Token with signature, or failure.
     */
    fun generateToken(amount: String, txnType: String = "PAYMENT"): Result<TransactionToken> {
        val deviceId = deviceIdProvider()
        val nonce = generateNonce()
        val timestamp = System.currentTimeMillis()
        val payload = buildCanonicalPayload(deviceId, amount, nonce, timestamp, txnType)
        val payloadBytes = payload.toByteArray(Charsets.UTF_8)
        val signResult = signer.sign(payloadBytes)
        return signResult.map { signature ->
            TransactionToken(
                deviceId = deviceId,
                amount = amount,
                nonce = nonce,
                timestamp = timestamp,
                txnType = txnType,
                signature = signature
            )
        }
    }

    /**
     * Serialize token to JSON string for APDU payload.
     */
    fun tokenToJson(token: TransactionToken): String = json.encodeToString(token)
}
