package com.example.cardreaderapp

import com.example.cardreaderapp.data.backend.MockBackendService
import com.example.cardreaderapp.data.crypto.KeystoreSigner
import com.example.cardreaderapp.data.crypto.TokenGenerator
import com.example.cardreaderapp.data.local.DeviceIdProvider
import com.example.cardreaderapp.domain.repository.TransactionRepository

/**
 * Application-scoped dependencies so FCM and MainActivity share the same mock backend.
 */
class CardReaderApplication : android.app.Application() {

    val mockBackend: MockBackendService by lazy { MockBackendService(maxAutoApproveAmountPkr = 50_000L) }
    val transactionRepository: TransactionRepository by lazy {
        com.example.cardreaderapp.data.repository.TransactionRepositoryImpl(mockBackend)
    }
    val tokenGenerator: TokenGenerator by lazy {
        val signer = KeystoreSigner()
        TokenGenerator(signer) { DeviceIdProvider.getDeviceId(this) }
    }
}
