package com.example.cardreaderapp.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import javax.security.auth.x500.X500Principal

/**
 * ECC signing using Android Keystore. Private key is non-exportable.
 * Used to sign transaction payloads for the reader app.
 */
class KeystoreSigner(private val keyAlias: String = "CardReaderTxKey") {

    private val tag = "KeystoreSigner"

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN
            )
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setKeyValidityForOriginationEnd(java.util.Date(Long.MAX_VALUE))
                .setCertificateSubject(X500Principal("CN=CardReaderApp"))
                .build()
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            keyPairGenerator.initialize(keyGenParameterSpec)
            keyPairGenerator.generateKeyPair()
            Log.d(tag, "Generated ECC key pair: $keyAlias")
        }
    }

    /**
     * Sign payload bytes with SHA256withECDSA. Returns Base64-encoded signature.
     */
    fun sign(payload: ByteArray): Result<String> {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.PrivateKeyEntry
                ?: return Result.failure(IllegalStateException("Key entry not found: $keyAlias"))
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(entry.privateKey)
            signature.update(payload)
            val sigBytes = signature.sign()
            Result.success(Base64.encodeToString(sigBytes, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.e(tag, "Sign failed", e)
            Result.failure(e)
        }
    }
}
