package com.example.cardreaderapp.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardreaderapp.data.nfc.ApduBuilder
import com.example.cardreaderapp.data.nfc.ApduParser
import com.example.cardreaderapp.data.nfc.NfcManager
import com.example.cardreaderapp.domain.model.BackendDecision
import com.example.cardreaderapp.domain.model.TransactionState
import com.example.cardreaderapp.domain.model.TransactionToken
import com.example.cardreaderapp.domain.repository.TransactionRepository
import com.example.cardreaderapp.data.crypto.TokenGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.nfc.Tag

/**
 * MVVM ViewModel for transaction flow: amount, token generation, NFC send, backend submit.
 */
class TransactionViewModel(
    private val tokenGenerator: TokenGenerator,
    private val nfcManager: NfcManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)
    val transactionState: StateFlow<TransactionState> = _transactionState.asStateFlow()

    private val _lastResponseMessage = MutableStateFlow<String?>(null)
    val lastResponseMessage: StateFlow<String?> = _lastResponseMessage.asStateFlow()

    private val _lastBackendResult = MutableStateFlow<Pair<BackendDecision, String?>?>(null)
    val lastBackendResult: StateFlow<Pair<BackendDecision, String?>?> = _lastBackendResult.asStateFlow()

    fun setAmount(value: String) {
        _amount.value = value
    }

    /**
     * Start transaction: generate token, submit to backend, then wait for NFC tap.
     */
    fun initiateTransaction() {
        val amt = _amount.value.trim()
        if (amt.isEmpty()) {
            _transactionState.value = TransactionState.NfcError("Enter amount")
            return
        }
        viewModelScope.launch {
            _transactionState.value = TransactionState.Initiating
            val tokenResult = tokenGenerator.generateToken(amt)
            tokenResult.fold(
                onSuccess = { token ->
                    _transactionState.value = TransactionState.WaitingForTap
                    submitToBackendAndStoreToken(token)
                },
                onFailure = {
                    _transactionState.value = TransactionState.NfcError("Token generation failed: ${it.message}")
                }
            )
        }
    }

    private var lastToken: TransactionToken? = null

    private suspend fun submitToBackendAndStoreToken(token: TransactionToken) {
        lastToken = token
        transactionRepository.submitToBackend(token).fold(
            onSuccess = { (decision, message) ->
                _lastBackendResult.value = Pair(decision, message)
                _transactionState.value = TransactionState.BackendResult(decision, message)
                _lastResponseMessage.value = message
                _transactionState.value = TransactionState.WaitingForTap
            },
            onFailure = {
                _lastResponseMessage.value = it.message
                _transactionState.value = TransactionState.NfcError("Backend: ${it.message}")
            }
        )
    }

    /**
     * Called when NFC tag is discovered (from Activity). Send token over ISO-DEP.
     */
    fun onNfcTagDiscovered(tag: Tag?) {
        val isoDep = nfcManager.getIsoDep(tag)
        if (isoDep == null) {
            _transactionState.value = TransactionState.NfcError("ISO-DEP not supported on this tag")
            return
        }
        val token = lastToken
        if (token == null) {
            nfcManager.closeQuietly(isoDep)
            _transactionState.value = TransactionState.NfcError("No transaction in progress")
            return
        }
        viewModelScope.launch {
            _transactionState.value = TransactionState.SendingOverNfc
            val json = tokenGenerator.tokenToJson(token)
            val payload = json.toByteArray(Charsets.UTF_8)
            if (payload.size > 255) {
                _transactionState.value = TransactionState.NfcError("Token too large for APDU")
                nfcManager.closeQuietly(isoDep)
                return@launch
            }
            val command = ApduBuilder.buildSendTokenCommand(payload)
            val result = nfcManager.transceive(isoDep, command)
            result.fold(
                onSuccess = { response ->
                    val parsed = ApduParser.parseResponse(response)
                    _transactionState.value = TransactionState.ResponseReceived
                    _lastResponseMessage.value = "NFC: ${parsed.statusMessage} (${parsed.statusCode})"
                },
                onFailure = {
                    _transactionState.value = TransactionState.NfcError("NFC error: ${it.message}")
                }
            )
        }
    }

    fun resetState() {
        _transactionState.value = TransactionState.Idle
        _lastResponseMessage.value = null
        _lastBackendResult.value = null
        lastToken = null
    }

    fun isNfcAvailable(adapter: android.nfc.NfcAdapter?): Boolean = nfcManager.isNfcAvailable(adapter)
}
