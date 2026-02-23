package com.finoria.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finoria.app.notifications.WeeklyReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class FinoriaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleWeeklyReminder()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            WeeklyReminderWorker.CHANNEL_ID,
            "Rappels Finoria",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Rappels hebdomadaires pour saisir vos transactions"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun scheduleWeeklyReminder() {
        val now = LocalDateTime.now()
        var nextSunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .withHour(20).withMinute(0).withSecond(0)
        if (nextSunday.isBefore(now)) {
            nextSunday = nextSunday.plusWeeks(1)
        }
        val delay = Duration.between(now, nextSunday).toMillis()

        val request = PeriodicWorkRequestBuilder<WeeklyReminderWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "weekly_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }
}
