package com.example.guardian.core.provisioning

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvisioningPayloadTest {

    private val component =
        "com.example.guardian.child/com.example.guardian.child.admin.GuardianDeviceAdminReceiver"

    @Test
    fun `includes required keys`() {
        val json = ProvisioningPayload.build(
            componentName = component,
            apkDownloadUrl = "https://example.com/child.apk",
            signatureChecksum = "AbC-123_xyz",
        )
        assertTrue(json.contains("PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"))
        assertTrue(json.contains("PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"))
        assertTrue(json.contains("PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"))
        assertTrue(json.contains("https://example.com/child.apk"))
        assertTrue(json.startsWith("{") && json.endsWith("}"))
    }

    @Test
    fun `omits optional wifi and extras when not provided`() {
        val json = ProvisioningPayload.build(component, "https://x/y.apk", "sum")
        assertFalse(json.contains("PROVISIONING_WIFI_SSID"))
        assertFalse(json.contains("PROVISIONING_ADMIN_EXTRAS_BUNDLE"))
    }

    @Test
    fun `embeds pairing code inside admin extras bundle`() {
        val json = ProvisioningPayload.build(
            component, "https://x/y.apk", "sum", pairingCode = "123456",
        )
        assertTrue(json.contains("PROVISIONING_ADMIN_EXTRAS_BUNDLE"))
        assertTrue(json.contains("\"pairingCode\":\"123456\""))
    }

    @Test
    fun `escapes quotes and backslashes in values`() {
        val json = ProvisioningPayload.build(
            component, "https://x/y.apk", "sum", wifiSsid = "My \"Home\" \\ Net",
        )
        assertTrue(json.contains("""My \"Home\" \\ Net"""))
    }
}
