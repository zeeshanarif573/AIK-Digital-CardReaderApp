package com.example.cardreaderapp.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Signed transaction token sent over NFC and to the backend.
 * Canonical payload for signing: deviceId|amount|nonce|timestamp|txnType
 */
@Serializable
data class TransactionToken(
    val deviceId: String,
    val amount: String,
    val nonce: String,
    val timestamp: Long,
    @SerialName("txnType") val txnType: String,
    val signature: String
)
