package com.finoria.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.finoria.R
import java.util.concurrent.TimeUnit

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            "finoria_reminders",
            "Rappels Finoria",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, "finoria_reminders")
            .setContentTitle("C'est l'heure des comptes !")
            .setContentText("N'oubliez pas de saisir vos dernières transactions.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(1, notification)
        return Result.success()
    }
}

object NotificationScheduler {
    fun scheduleWeeklyReminder(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "weekly_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
