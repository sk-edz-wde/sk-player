package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.MusicRepository

class SkPlayerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var musicRepository: MusicRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        musicRepository = MusicRepository(this, database)
    }
}
