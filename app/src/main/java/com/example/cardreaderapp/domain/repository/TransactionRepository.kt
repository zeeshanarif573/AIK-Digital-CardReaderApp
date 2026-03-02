package com.example.cardreaderapp.domain.repository

import com.example.cardreaderapp.domain.model.ApduResponse
import com.example.cardreaderapp.domain.model.BackendDecision
import com.example.cardreaderapp.domain.model.TransactionToken

/**
 * Repository for NFC transaction flow and backend submission.
 */
interface TransactionRepository {

    /**
     * Submit token to mock backend (online flow). Returns backend decision.
     */
    suspend fun submitToBackend(token: TransactionToken): Result<Pair<BackendDecision, String?>>

    /**
     * Send user consent decision for a pending transaction to mock backend.
     * transactionId is the nonce of the pending transaction.
     */
    suspend fun sendConsentDecision(transactionId: String, approved: Boolean): Result<Unit>
}
