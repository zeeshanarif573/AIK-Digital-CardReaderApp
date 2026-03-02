package com.example.cardreaderapp.data.backend

import com.example.cardreaderapp.domain.model.BackendDecision
import com.example.cardreaderapp.domain.model.TransactionToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-app mock backend: verifies signature (simulated), checks nonce reuse, approves by amount limit.
 * Returns APPROVED / DECLINED / PENDING_CONSENT. Handles consent decisions by nonce.
 * Amount is in PKR (Pakistani Rupee).
 */
class MockBackendService(
    private val maxAutoApproveAmountPkr: Long = 50_000L // 50,000 PKR
) {
    private val mutex = Mutex()
    private val usedNonces = mutableSetOf<String>()
    private val pendingNonces = mutableSetOf<String>()
    private val consentResults = mutableMapOf<String, Boolean>() // nonce -> approved

    /**
     * Submit token: simulated verify, nonce check, amount check.
     * Amount in token is in PKR (string, e.g. "500" or "1250.50").
     */
    suspend fun submitToken(token: TransactionToken): Pair<BackendDecision, String?> = mutex.withLock {
        if (token.nonce in usedNonces) {
            return Pair(BackendDecision.DECLINED, "Nonce reuse detected")
        }
        if (!verifySignatureSimulated(token)) {
            return Pair(BackendDecision.DECLINED, "Invalid signature")
        }
        val amountPkr = token.amount.toDoubleOrNull()?.toLong() ?: 0L
        return when {
            amountPkr <= maxAutoApproveAmountPkr -> {
                usedNonces.add(token.nonce)
                Pair(BackendDecision.APPROVED, null)
            }
            else -> {
                usedNonces.add(token.nonce)
                pendingNonces.add(token.nonce)
                Pair(BackendDecision.PENDING_CONSENT, "Consent required")
            }
        }
    }

    /**
     * Simulated verification (no real crypto in mock).
     */
    private fun verifySignatureSimulated(token: TransactionToken): Boolean {
        return token.signature.isNotBlank() && token.deviceId.isNotBlank()
    }

    /**
     * Submit user consent for a pending transaction (by nonce).
     */
    suspend fun submitConsent(nonce: String, approved: Boolean): Boolean = mutex.withLock {
        if (nonce !in pendingNonces) return false
        pendingNonces.remove(nonce)
        consentResults[nonce] = approved
        return true
    }

    fun getConsentDecision(nonce: String): Boolean? = consentResults[nonce]
}
