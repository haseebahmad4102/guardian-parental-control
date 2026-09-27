package com.example.guardian.child.di

import android.content.Context
import androidx.room.Room
import com.example.guardian.child.data.local.GuardianDatabase
import com.example.guardian.child.data.local.PolicyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GuardianDatabase =
        Room.databaseBuilder(context, GuardianDatabase::class.java, "guardian.db").build()

    @Provides
    fun providePolicyDao(database: GuardianDatabase): PolicyDao = database.policyDao()
}
