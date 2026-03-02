package com.example.cardreaderapp

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.PermissionChecker
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardreaderapp.data.nfc.NfcManager
import com.example.cardreaderapp.di.TransactionViewModelFactory
import com.example.cardreaderapp.presentation.transaction.TransactionScreen
import com.example.cardreaderapp.presentation.transaction.TransactionViewModel
import com.example.cardreaderapp.ui.theme.CardReaderAppTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var viewModelFactory: TransactionViewModelFactory? = null

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            PermissionChecker.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PermissionChecker.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val app = application as CardReaderApplication
        val tokenGenerator = app.tokenGenerator
        val repository = app.transactionRepository
        val nfcManager = NfcManager()
        viewModelFactory = TransactionViewModelFactory(tokenGenerator, nfcManager, repository)

        enableEdgeToEdge()
        setContent {
            CardReaderAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TransactionScreen(
                        viewModel = viewModel(factory = viewModelFactory!!)
                    )
                }
            }
        }
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val intent = Intent(this, javaClass).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
                val filters = arrayOf(
                    IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                )
                val techLists = arrayOf(arrayOf(IsoDep::class.java.name))
                adapter.enableForegroundDispatch(this, pendingIntent, filters, techLists)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        ) {
            @Suppress("DEPRECATION")
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            viewModelFactory?.let { factory ->
                ViewModelProvider(this, factory).get(TransactionViewModel::class.java)
                    .onNfcTagDiscovered(tag)
            }
        }
    }
}
