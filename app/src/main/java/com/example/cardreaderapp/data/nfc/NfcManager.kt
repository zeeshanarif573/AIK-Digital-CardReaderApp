package com.example.cardreaderapp.data.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Manages NFC reader mode (ISO-DEP): connection lifecycle, send APDU, receive response.
 * Independent of UI; call from ViewModel or use case.
 */
class NfcManager {

    private val tag = "NfcManager"

    /**
     * Check if NFC is available and enabled.
     */
    fun isNfcAvailable(adapter: NfcAdapter?): Boolean = adapter != null && adapter.isEnabled

    /**
     * Get IsoDep from discovered tag (from Activity intent).
     */
    fun getIsoDep(tag: Tag?): IsoDep? = tag?.let { IsoDep.get(it) }

    /**
     * Connect to tag, send command APDU, receive response, then disconnect.
     * @param isoDep IsoDep from NfcAdapter / Tag
     * @param commandApdu APDU built by ApduBuilder
     * @return response bytes or failure
     */
    suspend fun transceive(isoDep: IsoDep, commandApdu: ByteArray): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                if (!isoDep.isConnected) {
                    isoDep.connect()
                }
                val timeout = isoDep.timeout
                isoDep.timeout = 3000
                val response = isoDep.transceive(commandApdu)
                isoDep.timeout = timeout
                isoDep.close()
                Result.success(response)
            } catch (e: IOException) {
                Log.e(tag, "NFC transceive error", e)
                try {
                    isoDep.close()
                } catch (_: IOException) { }
                Result.failure(e)
            }
        }

    /**
     * Close connection if open (e.g. on error path).
     */
    fun closeQuietly(isoDep: IsoDep?) {
        try {
            isoDep?.close()
        } catch (_: IOException) { }
    }
}
