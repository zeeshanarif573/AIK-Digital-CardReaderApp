package com.example.cardreaderapp.data.repository

import com.example.cardreaderapp.domain.model.BackendDecision
import com.example.cardreaderapp.domain.model.TransactionToken
import com.example.cardreaderapp.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation using in-app MockBackendService. Consent uses transactionId as nonce.
 */
class TransactionRepositoryImpl(
    private val mockBackend: com.example.cardreaderapp.data.backend.MockBackendService
) : TransactionRepository {

    override suspend fun submitToBackend(token: TransactionToken): Result<Pair<BackendDecision, String?>> =
        withContext(Dispatchers.IO) {
            try {
                val (decision, message) = mockBackend.submitToken(token)
                Result.success(Pair(decision, message))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun sendConsentDecision(transactionId: String, approved: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val ok = mockBackend.submitConsent(transactionId, approved)
                if (ok) Result.success(Unit) else Result.failure(IllegalArgumentException("Transaction not found or already decided"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
