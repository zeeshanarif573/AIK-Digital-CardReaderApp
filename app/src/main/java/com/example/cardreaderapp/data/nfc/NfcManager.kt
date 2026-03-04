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
     * Connect, send SELECT AID, then if success (90 00) send payment command and return its response.
     * This avoids 0x6986 (command not allowed / no application selected) when talking to HCE.
     */
    suspend fun transceiveSelectThenPayment(
        isoDep: IsoDep,
        selectApdu: ByteArray,
        paymentApdu: ByteArray
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (!isoDep.isConnected) isoDep.connect()
            isoDep.timeout = 3000
            val selectResponse = isoDep.transceive(selectApdu)
            if (selectResponse.size < 2) {
                try { isoDep.close() } catch (_: Exception) { }
                return@withContext Result.failure(IOException("SELECT response too short"))
            }
            val sw1 = selectResponse[selectResponse.size - 2].toInt() and 0xFF
            val sw2 = selectResponse[selectResponse.size - 1].toInt() and 0xFF
            if (sw1 != 0x90 || sw2 != 0x00) {
                try { isoDep.close() } catch (_: Exception) { }
                return@withContext Result.failure(IOException("SELECT failed: SW1=$sw1 SW2=$sw2"))
            }
            val paymentResponse = isoDep.transceive(paymentApdu)
            try { isoDep.close() } catch (e: Exception) { Log.w(tag, "Error closing IsoDep", e) }
            Result.success(paymentResponse)
        } catch (e: IOException) {
            Log.e(tag, "NFC transceive IO error", e)
            try { isoDep.close() } catch (_: Exception) { }
            Result.failure(e)
        } catch (e: SecurityException) {
            Log.e(tag, "NFC transceive security error", e)
            try { isoDep.close() } catch (_: Exception) { }
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Unexpected NFC transceive error", e)
            try { isoDep.close() } catch (_: Exception) { }
            Result.failure(e)
        }
    }

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
                val originalTimeout = isoDep.timeout
                isoDep.timeout = 3000
                val response = isoDep.transceive(commandApdu)
                isoDep.timeout = originalTimeout
                // Always try to close, but never let close() crash the app
                try {
                    isoDep.close()
                } catch (closeError: Exception) {
                    // Includes TagLostException, SecurityException: Tag out of date, etc.
                    Log.w(tag, "Error while closing IsoDep", closeError)
                }
                Result.success(response)
            } catch (e: IOException) {
                Log.e(tag, "NFC transceive IO error", e)
                try {
                    isoDep.close()
                } catch (closeError: Exception) {
                    Log.w(tag, "Error while closing IsoDep after IO error", closeError)
                }
                Result.failure(e)
            } catch (e: SecurityException) {
                // e.g. "Permission Denial: Tag ... is out of date"
                Log.e(tag, "NFC transceive security error (tag likely out of date)", e)
                try {
                    isoDep.close()
                } catch (closeError: Exception) {
                    Log.w(tag, "Error while closing IsoDep after security error", closeError)
                }
                Result.failure(e)
            } catch (e: Exception) {
                // Catch-all so that any unexpected runtime exception is surfaced as a failure Result
                Log.e(tag, "Unexpected NFC transceive error", e)
                try {
                    isoDep.close()
                } catch (closeError: Exception) {
                    Log.w(tag, "Error while closing IsoDep after unexpected error", closeError)
                }
                Result.failure(e)
            }
        }

    /**
     * Close connection if open (e.g. on error path).
     */
    fun closeQuietly(isoDep: IsoDep?) {
        try {
            isoDep?.close()
        } catch (_: Exception) { }
    }
}
