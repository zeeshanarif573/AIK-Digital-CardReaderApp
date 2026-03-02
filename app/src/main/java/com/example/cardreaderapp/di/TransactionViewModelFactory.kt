package com.example.cardreaderapp.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cardreaderapp.presentation.transaction.TransactionViewModel

/**
 * Factory for TransactionViewModel with constructor dependencies.
 */
class TransactionViewModelFactory(
    private val tokenGenerator: com.example.cardreaderapp.data.crypto.TokenGenerator,
    private val nfcManager: com.example.cardreaderapp.data.nfc.NfcManager,
    private val transactionRepository: com.example.cardreaderapp.domain.repository.TransactionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != TransactionViewModel::class.java) {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
        return TransactionViewModel(tokenGenerator, nfcManager, transactionRepository) as T
    }
}
