package com.jainhardik120.automation.di

import android.content.Context
import com.jainhardik120.automation.data.ServiceConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideServiceConnector(
        @ApplicationContext context: Context
    ): ServiceConnector {
        return ServiceConnector(context)
    }
}
