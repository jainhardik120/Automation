package com.jainhardik120.automation.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.jainhardik120.automation.R
import com.jainhardik120.automation.data.ble_service.ServiceConnector
import com.jainhardik120.automation.data.database.MacropadDatabase
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

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(
            context.resources.getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
    }


    @Provides
    @Singleton
    fun provideDatabase(
        app: Application
    ): MacropadDatabase {
        return Room.databaseBuilder(app, MacropadDatabase::class.java, "macropad_database")
            .fallbackToDestructiveMigration().build()
    }

}
