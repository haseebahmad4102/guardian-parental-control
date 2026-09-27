package com.example.guardian.child.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single cached policy row (id is always [SINGLETON_ID]). The nested [com.example.guardian
 * .core.model.Policy] is stored as a JSON string, so Room needs no type converters for its maps
 * and lists. See [com.example.guardian.child.data.local.PolicyJson].
 */
@Entity(tableName = "policy")
data class PolicyEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val json: String,
    val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
