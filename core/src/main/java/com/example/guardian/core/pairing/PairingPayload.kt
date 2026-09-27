package com.example.guardian.core.pairing

/**
 * Encodes/decodes the payload embedded in the pairing QR code. The parent renders
 * [encode]; the child scanner passes whatever it read to [decode], which tolerates both the
 * full URI form and a bare 6-digit code (in case the user typed it or scanned a plain code).
 *
 * Form: `guardian://pair?code=123456`
 */
object PairingPayload {
    const val SCHEME = "guardian"
    private const val PREFIX = "$SCHEME://pair?code="

    fun encode(code: String): String = "$PREFIX$code"

    /** Returns the valid 6-digit code found in [raw], or null if none. */
    fun decode(raw: String?): String? {
        if (raw == null) return null
        val trimmed = raw.trim()
        val candidate = when {
            trimmed.startsWith(PREFIX) -> trimmed.removePrefix(PREFIX)
            else -> PairingCode.normalize(trimmed)
        }
        return candidate.takeIf { PairingCode.isValid(it) }
    }
}
