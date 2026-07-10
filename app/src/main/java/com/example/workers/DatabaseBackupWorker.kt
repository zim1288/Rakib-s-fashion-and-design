package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dbName = "tally_khata_database"
            val dbFile = applicationContext.getDatabasePath(dbName)
            val walFile = applicationContext.getDatabasePath("$dbName-wal")
            val shmFile = applicationContext.getDatabasePath("$dbName-shm")

            if (!dbFile.exists()) {
                sendNotification("Backup Failed", "Database file not found.")
                return Result.failure()
            }

            // 1. & 4. Scoped Storage & Security: App-specific external storage.
            // Other apps cannot access this without root, ensuring backup security.
            val backupDir = File(
                applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "RakibTallyBackups_Secure"
            )

            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            // Backup main DB
            val backupFile = File(backupDir, "secure_backup_$dateStr.db")
            copyFile(dbFile, backupFile)

            // Backup WAL if exists
            if (walFile.exists()) {
                val backupWalFile = File(backupDir, "secure_backup_$dateStr.db-wal")
                copyFile(walFile, backupWalFile)
            }

            // Backup SHM if exists
            if (shmFile.exists()) {
                val backupShmFile = File(backupDir, "secure_backup_$dateStr.db-shm")
                copyFile(shmFile, backupShmFile)
            }

            // 2. User Feedback for Background Tasks
            sendNotification("Backup Successful", "Secure backup completed at $dateStr")

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            sendNotification("Backup Failed", "An error occurred: ${e.localizedMessage}")
            Result.failure()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "backup_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Backup Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun copyFile(source: File, dest: File) {
        FileInputStream(source).use { inStream ->
            FileOutputStream(dest).use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }
}
