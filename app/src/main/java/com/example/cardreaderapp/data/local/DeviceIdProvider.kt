package com.example.cardreaderapp.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Provides a stable device identifier for transaction tokens.
 * Uses Android ID; in production consider a server-bound or attestation-based ID.
 */
object DeviceIdProvider {

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
        return "reader_${androidId}"
    }
}
