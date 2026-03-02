package com.example.cardreaderapp.domain.model

/**
 * Parsed response from receiver device over ISO-DEP.
 */
data class ApduResponse(
    val statusCode: Int,
    val statusMessage: String,
    val data: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ApduResponse
        if (statusCode != other.statusCode) return false
        if (statusMessage != other.statusMessage) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}
