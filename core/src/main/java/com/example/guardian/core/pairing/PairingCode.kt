package com.example.guardian.core.pairing

import kotlin.random.Random

/**
 * Generates and validates the 6-digit codes used to pair a child device with a parent
 * account. Full QR provisioning wraps the same code payload (implemented in a later phase).
 *
 * Digits only, always length [LENGTH], zero-padded — so "004213" is valid.
 */
object PairingCode {
    const val LENGTH = 6
    private val PATTERN = Regex("^\\d{$LENGTH}$")

    fun generate(random: Random = Random.Default): String {
        val max = 1_000_000 // 10^LENGTH
        return random.nextInt(0, max).toString().padStart(LENGTH, '0')
    }

    fun isValid(code: String?): Boolean = code != null && PATTERN.matches(code)

    /** Normalizes user input (trims, strips spaces) before validation. */
    fun normalize(raw: String): String = raw.trim().replace(" ", "")
}
