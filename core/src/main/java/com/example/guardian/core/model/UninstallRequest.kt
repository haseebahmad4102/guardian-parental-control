package com.example.guardian.core.model

/** State of a child-initiated request to uninstall Guardian. Path: `devices/{deviceId}/meta/uninstall`. */
enum class UninstallStatus {
    /** No request outstanding. */
    NONE,
    /** Child attempted to uninstall; waiting on the parent's decision. */
    PENDING,
    /** Parent approved — the child may uninstall (protection is dropped). */
    ALLOWED,
    /** Parent refused — protection stays on. */
    DENIED,
    UNKNOWN;

    companion object {
        fun fromValue(value: String?): UninstallStatus =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

data class UninstallRequest(
    val status: String = UninstallStatus.NONE.name,
    val requestedAt: Long = 0L,
    val decidedAt: Long = 0L,
) {
    val uninstallStatus: UninstallStatus get() = UninstallStatus.fromValue(status)
}
