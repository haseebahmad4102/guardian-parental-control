package com.example.guardian.core.provisioning

/**
 * Builds the JSON payload embedded in a **Device Owner provisioning QR code**. Scanning it on a
 * factory-reset device (tap the setup-wizard welcome screen 6 times to open the QR reader)
 * downloads the child APK and sets it as Device Owner.
 *
 * The JSON is assembled by hand (no `org.json`) so this stays pure Kotlin and unit-testable.
 * See `docs/DEVICE_OWNER_PROVISIONING.md` for how to obtain the checksum and host the APK.
 */
object ProvisioningPayload {

    // Standard android.app.extra.* provisioning keys.
    private const val COMPONENT = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
    private const val CHECKSUM = "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"
    private const val DOWNLOAD = "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"
    private const val SKIP_ENCRYPTION = "android.app.extra.PROVISIONING_SKIP_ENCRYPTION"
    private const val WIFI_SSID = "android.app.extra.PROVISIONING_WIFI_SSID"
    private const val WIFI_PASSWORD = "android.app.extra.PROVISIONING_WIFI_PASSWORD"
    private const val ADMIN_EXTRAS = "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE"

    /**
     * @param componentName flattened admin component, e.g.
     *   `com.example.guardian.child/com.example.guardian.child.admin.GuardianDeviceAdminReceiver`
     * @param apkDownloadUrl HTTPS URL hosting the exact child APK to install
     * @param signatureChecksum URL-safe base64 SHA-256 of the APK's signing cert
     * @param wifiSsid / wifiPassword optional network to join during provisioning
     * @param pairingCode optional 6-digit code passed through to the DPC so the device can
     *   auto-pair after provisioning (delivered via the admin extras bundle)
     */
    fun build(
        componentName: String,
        apkDownloadUrl: String,
        signatureChecksum: String,
        wifiSsid: String? = null,
        wifiPassword: String? = null,
        pairingCode: String? = null,
        skipEncryption: Boolean = true,
    ): String {
        val fields = LinkedHashMap<String, Any>()
        fields[COMPONENT] = componentName
        fields[DOWNLOAD] = apkDownloadUrl
        fields[CHECKSUM] = signatureChecksum
        fields[SKIP_ENCRYPTION] = skipEncryption
        if (!wifiSsid.isNullOrBlank()) {
            fields[WIFI_SSID] = wifiSsid
            if (!wifiPassword.isNullOrBlank()) fields[WIFI_PASSWORD] = wifiPassword
        }
        if (!pairingCode.isNullOrBlank()) {
            fields[ADMIN_EXTRAS] = linkedMapOf<String, Any>("pairingCode" to pairingCode)
        }
        return encodeObject(fields)
    }

    @Suppress("UNCHECKED_CAST")
    private fun encodeObject(map: Map<String, Any>): String =
        map.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${quote(key)}:${encodeValue(value)}"
        }

    @Suppress("UNCHECKED_CAST")
    private fun encodeValue(value: Any): String = when (value) {
        is Boolean -> value.toString()
        is Number -> value.toString()
        is Map<*, *> -> encodeObject(value as Map<String, Any>)
        else -> quote(value.toString())
    }

    private fun quote(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }
}
