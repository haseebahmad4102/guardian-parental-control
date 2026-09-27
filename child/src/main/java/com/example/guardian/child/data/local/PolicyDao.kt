package com.example.guardian.child.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PolicyDao {

    @Query("SELECT * FROM policy WHERE id = :id")
    fun observe(id: Int = PolicyEntity.SINGLETON_ID): Flow<PolicyEntity?>

    @Query("SELECT * FROM policy WHERE id = :id")
    suspend fun get(id: Int = PolicyEntity.SINGLETON_ID): PolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PolicyEntity)
}
