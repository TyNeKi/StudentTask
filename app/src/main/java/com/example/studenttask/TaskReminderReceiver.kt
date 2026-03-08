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

        val builder = NotificationCompat.Builder(context, "STUDENT_TASK_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Student Task Tracker")
            .setContentText("Reminder: $title")
            // REQUIRED for Pop-up (Heads-up)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // REQUIRED for Lockscreen visibility
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(101, builder.build())
        }
    }
}