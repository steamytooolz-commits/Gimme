package com.example

import android.app.Application
import androidx.room.Room
import com.example.api.OpenAiCompatibleLlmClient
import com.example.data.local.ThemisDatabase
import com.example.data.repository.GameRepository
import com.example.data.repository.SecureStorageRepositoryImpl
import com.google.android.gms.ads.MobileAds

/**
 * Custom application class to initialize the AdMob SDK once per app lifecycle
 * and implement a clean Service Locator pattern for Dependency Injection (DI).
 */
class ThemisApplication : Application() {

    lateinit var database: ThemisDatabase
        private set

    lateinit var repository: GameRepository
        private set

    lateinit var secureStorageRepository: SecureStorageRepositoryImpl
        private set

    lateinit var llmClient: OpenAiCompatibleLlmClient
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize AdMob once in Application subclass to prevent recreation re-init
        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Instantiate core database and dependencies (Clean Service Locator pattern)
        database = Room.databaseBuilder(
            applicationContext,
            ThemisDatabase::class.java,
            "themis_db"
        )
        // Enable fallback migrations but log potential structural modifications
        .fallbackToDestructiveMigration()
        .build()

        repository = GameRepository(database)
        secureStorageRepository = SecureStorageRepositoryImpl(applicationContext)
        llmClient = OpenAiCompatibleLlmClient(applicationContext)
    }
}
