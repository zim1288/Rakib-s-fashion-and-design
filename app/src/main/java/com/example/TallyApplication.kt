package com.example

import android.app.Application
import androidx.room.Room
import com.example.db.TallyDatabase
import com.example.db.TallyRepository

class TallyApplication : Application() {
    lateinit var database: TallyDatabase
    lateinit var repository: TallyRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        database = Room.databaseBuilder(
            applicationContext,
            TallyDatabase::class.java,
            "tally_khata_database"
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

        repository = TallyRepository(database.tallyDao())
    }

    companion object {
        lateinit var instance: TallyApplication
            private set
    }
}
