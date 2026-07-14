package com.example

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.db.TallyDatabase
import com.example.db.TallyRepository
import com.example.workers.BackgroundSyncWorker
import com.example.workers.DatabaseBackupWorker
import java.util.concurrent.TimeUnit

class TallyApplication : Application() {

    lateinit var database: TallyDatabase
    lateinit var repository: TallyRepository

    override fun onCreate() {
        super.onCreate()
        instance = this

        val migration6to7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE saree_inventory ADD COLUMN local_image_url TEXT")
                } catch (e: Exception) {
                    // Ignore if column already exists
                }
                try {
                    db.execSQL("ALTER TABLE stock_production ADD COLUMN local_image_url TEXT")
                } catch (e: Exception) {
                    // Ignore if column already exists
                }
            }
        }

        val migration8to9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE transaction_logs ADD COLUMN customer_number TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // Ignore if column already exists
                }
            }
        }

        database = Room.databaseBuilder(
            applicationContext,
            TallyDatabase::class.java,
            "tally_khata_database"
        )
            .addMigrations(migration6to7, migration8to9)
            .fallbackToDestructiveMigration(true)
            // 3. Database Migration Strategy:
            // Destructive migration is removed for production to ensure no data loss occurs
            // if a migration is missed. All future schema changes MUST be handled via addMigrations().
            .build()

        repository = TallyRepository(database.tallyDao())

        setupWorkers()
    }

    private fun setupWorkers() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<DatabaseBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DatabaseBackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )

        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(syncConstraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "BackgroundSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        lateinit var instance: TallyApplication
            private set
    }
}
