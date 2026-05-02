package com.example.studenttask

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Task Reminder"

        // Use taskId as notification ID so multiple reminders don't overwrite each other.
        // Falls back to a fixed ID if taskId is missing.
        val taskId = intent.getLongExtra("taskId", 0L)
        val notificationId = if (taskId != 0L) taskId.toInt() else 101

        val builder = NotificationCompat.Builder(context, "STUDENT_TASK_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("📚 Student Task Tracker")
            .setContentText("Reminder: $title")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Don't forget: $title is due now!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    }
}
