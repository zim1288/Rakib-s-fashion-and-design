package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.TallyApplication

class BackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("BackgroundSyncWorker", "Starting background sync...")

            // Sync with remote server (MongoDB) via API
            TallyApplication.instance.repository.syncOfflineData()

            Log.d("BackgroundSyncWorker", "Background sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
