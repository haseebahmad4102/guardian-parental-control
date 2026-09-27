package com.example.guardian.core.model

/**
 * A remote command queued by the parent. Firestore path: `devices/{deviceId}/commands/{autoId}`.
 * An FCM data push notifies the child to read the queue; the child also drains it on reconnect.
 *
 * [type] and [status] are stored as raw Strings and read through [commandType] / [commandStatus]
 * so an unrecognized value degrades to UNKNOWN instead of throwing during deserialization.
 */
data class Command(
    val id: String = "",
    val type: String = CommandType.UNKNOWN.name,
    val payload: Map<String, String> = emptyMap(),
    val status: String = CommandStatus.PENDING.name,
    val createdAt: Long = 0L,
    val executedAt: Long? = null,
    val error: String? = null,
) {
    val commandType: CommandType get() = CommandType.fromValue(type)
    val commandStatus: CommandStatus get() = CommandStatus.fromValue(status)
}
