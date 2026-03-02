package com.example.cardreaderapp.domain.model

/**
 * UI state for the current transaction flow.
 */
sealed class TransactionState {
    data object Idle : TransactionState()
    data object Initiating : TransactionState()
    data object WaitingForTap : TransactionState()
    data object SendingOverNfc : TransactionState()
    data object ResponseReceived : TransactionState()
    data class NfcError(val message: String) : TransactionState()
    data class BackendResult(val result: BackendDecision, val message: String?) : TransactionState()
}
