package com.example.cardreaderapp.data.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cardreaderapp.CardReaderApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles Approve/Reject actions from the consent notification and sends decision to mock backend.
 */
class ConsentActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        val transactionId = intent.getStringExtra(TransactionConsentService.EXTRA_TRANSACTION_ID) ?: return
        val approved = when (intent.action) {
            TransactionConsentService.ACTION_APPROVE -> true
            TransactionConsentService.ACTION_REJECT -> false
            else -> return
        }
        val app = context.applicationContext as? CardReaderApplication ?: return
        scope.launch {
            app.transactionRepository.sendConsentDecision(transactionId, approved).fold(
                onSuccess = { Log.d("ConsentAction", "Consent sent: $approved for $transactionId") },
                onFailure = { Log.e("ConsentAction", "Failed to send consent", it) }
            )
        }
    }
}
