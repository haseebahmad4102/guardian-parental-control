package com.example.guardian.child.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PolicyEntity::class], version = 1, exportSchema = false)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun policyDao(): PolicyDao
}
