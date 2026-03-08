package com.example.studenttask

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DashBoard : AppCompatActivity() {

    private lateinit var taskManager: TaskManager
    private lateinit var taskContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash_board)

        taskManager = TaskManager(this)
        taskContainer = findViewById(R.id.taskContainer)

        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        findViewById<Button>(R.id.addTaskBtn).setOnClickListener {
            startActivity(Intent(this, SaveTask::class.java))
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            taskManager.clearAllTasks()
            refreshTasks()
        }

        findViewById<Button>(R.id.sortBtn).setOnClickListener { refreshTasks() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "STUDENT_TASK_CHANNEL",
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTasks()
    }

    private fun refreshTasks() {
        taskContainer.removeAllViews()
        val tasks = taskManager.getTasks()

        if (tasks.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No tasks yet!"
                gravity = Gravity.CENTER
                setPadding(0, 100, 0, 0)
            }
            taskContainer.addView(emptyTv)
            return
        }

        for (task in tasks) {
            val card = CardView(this).apply {
                // FIX: Use MATCH_PARENT and WRAP_CONTENT in ALL CAPS
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 32)
                layoutParams = params

                radius = 24f
                cardElevation = 6f
                setCardBackgroundColor(if (task.status == "COMPLETED") Color.parseColor("#E8F5E9") else Color.WHITE)
                setOnClickListener { showTaskOptions(task) }
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 40, 48, 40)
            }

            val titleTv = TextView(this).apply {
                text = "${if (task.status == "COMPLETED") "✅ " else "⏳ "}${task.title}"
                textSize = 18f
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
            }

            val detailsTv = TextView(this).apply {
                text = "📅 ${task.date}  •  ⏰ ${task.time}"
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 0)
            }

            layout.addView(titleTv)
            layout.addView(detailsTv)
            card.addView(layout)
            taskContainer.addView(card)
        }
    }

    private fun showTaskOptions(task: Task) {
        val statusText = if (task.status == "PENDING") "Mark as Done" else "Mark as Pending"
        val descriptionText = if (task.description.isEmpty()) "No description." else task.description

        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setMessage("Due: ${task.date} ${task.time}\n\nDescription: $descriptionText")
            .setNeutralButton("Back") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Delete") { _, _ ->
                taskManager.deleteTask(task.id)
                refreshTasks()
            }
            .setPositiveButton(statusText) { _, _ ->
                val newStatus = if (task.status == "PENDING") "COMPLETED" else "PENDING"
                taskManager.updateTask(task.copy(status = newStatus))
                refreshTasks()
            }
            .show()
    }
}