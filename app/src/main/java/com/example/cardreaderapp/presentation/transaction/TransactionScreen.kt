package com.example.cardreaderapp.presentation.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cardreaderapp.domain.model.BackendDecision
import com.example.cardreaderapp.domain.model.TransactionState

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val amount by viewModel.amount.collectAsState()
    val state by viewModel.transactionState.collectAsState()
    val lastMessage by viewModel.lastResponseMessage.collectAsState()
    val lastBackend by viewModel.lastBackendResult.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NFC Transaction",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter amount (PKR) and send",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = viewModel::setAmount,
            label = { Text("Amount (PKR)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::initiateTransaction,
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is TransactionState.Initiating && state !is TransactionState.SendingOverNfc
        ) {
            Text(
                when (state) {
                    is TransactionState.Initiating -> "Preparing..."
                    is TransactionState.SendingOverNfc -> "Sending..."
                    else -> "Initiate & Send"
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = viewModel::resetState,
            modifier = Modifier.fillMaxWidth(),
            enabled = state != TransactionState.Idle
        ) {
            Text("Reset")
        }
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Transaction state",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stateDisplayText(state),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                lastBackend?.let { (decision, msg) ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Backend: $decision${msg?.let { " — $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (decision) {
                            BackendDecision.APPROVED -> MaterialTheme.colorScheme.primary
                            BackendDecision.DECLINED -> MaterialTheme.colorScheme.error
                            BackendDecision.PENDING_CONSENT -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
                lastMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun stateDisplayText(state: TransactionState): String = when (state) {
    TransactionState.Idle -> "Idle"
    TransactionState.Initiating -> "Initiating"
    TransactionState.WaitingForTap -> "Tap receiver device (NFC)"
    TransactionState.SendingOverNfc -> "Sending over NFC…"
    TransactionState.ResponseReceived -> "Response received"
    is TransactionState.NfcError -> "Error: ${state.message}"
    is TransactionState.BackendResult -> "Backend: ${state.result}"
}
