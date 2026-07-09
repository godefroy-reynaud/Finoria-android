package com.finoria.app.notifications

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finoria.app.R

class WeeklyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        showReminderNotification(applicationContext)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "weekly_reminder"
        const val NOTIFICATION_ID = 1001

        /**
         * Construit et affiche la notification de rappel hebdomadaire.
         *
         * Extrait de [doWork] pour être réutilisable — notamment par un déclencheur
         * de test manuel — et garantir que le test affiche exactement la même
         * notification que le rappel du dimanche.
         */
        fun showReminderNotification(context: Context) {
            // Intent launcher (ACTION_MAIN + CATEGORY_LAUNCHER) : ramène la tâche
            // existante au premier plan sans empiler une seconde MainActivity.
            val contentIntent = context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.let {
                    PendingIntent.getActivity(
                        context,
                        0,
                        it,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Rappel — Finoria")
                .setContentText("As-tu acheté quelque chose cette semaine ?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context)
                    .notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS permission non accordée
            }
        }
    }
}
