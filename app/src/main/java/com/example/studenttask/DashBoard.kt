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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DashBoard : AppCompatActivity() {

    private lateinit var taskManager: TaskManager
    private lateinit var taskContainer: LinearLayout

    // Cycles: "ALL" -> "PENDING" -> "COMPLETED"
    private var currentFilter = "ALL"

    // Cycles: "DATE" -> "PRIORITY"
    private var currentSort = "DATE"

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

        // Confirm before wiping all tasks
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            if (taskManager.getTasks().isEmpty()) {
                Toast.makeText(this, "No tasks to clear.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Clear All Tasks")
                .setMessage("Are you sure you want to delete all tasks? This cannot be undone.")
                .setPositiveButton("Clear All") { _, _ ->
                    taskManager.clearAllTasks()
                    refreshTasks()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Sort button cycles between DATE and PRIORITY sort
        val sortBtn = findViewById<Button>(R.id.sortBtn)
        sortBtn.setOnClickListener {
            currentSort = if (currentSort == "DATE") "PRIORITY" else "DATE"
            sortBtn.text = if (currentSort == "DATE") "Sort: Date" else "Sort: Priority"
            refreshTasks()
        }

        // Filter button cycles between ALL, PENDING, COMPLETED
        val filterBtn = findViewById<Button>(R.id.filterBtn)
        filterBtn.setOnClickListener {
            currentFilter = when (currentFilter) {
                "ALL" -> "PENDING"
                "PENDING" -> "COMPLETED"
                else -> "ALL"
            }
            filterBtn.text = when (currentFilter) {
                "PENDING" -> "Filter: Pending"
                "COMPLETED" -> "Filter: Done"
                else -> "Filter: All"
            }
            refreshTasks()
        }
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

        // Apply filter
        val filtered = when (currentFilter) {
            "PENDING" -> taskManager.getTasks().filter { it.status == "PENDING" }
            "COMPLETED" -> taskManager.getTasks().filter { it.status == "COMPLETED" }
            else -> taskManager.getTasks()
        }

        // Apply sort
        val tasks = if (currentSort == "PRIORITY") {
            taskManager.getSortedByPriority(filtered)
        } else {
            taskManager.getSortedByDate(filtered)
        }

        // Update task count label
        val pending = taskManager.getPendingCount()
        val total = taskManager.getTasks().size
        findViewById<TextView>(R.id.tvTaskCount).text = "$pending pending • $total total"

        if (tasks.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = if (currentFilter == "ALL") "No tasks yet! Tap + to add one." else "No ${currentFilter.lowercase()} tasks."
                gravity = Gravity.CENTER
                setPadding(0, 100, 0, 0)
                setTextColor(Color.GRAY)
                textSize = 16f
            }
            taskContainer.addView(emptyTv)
            return
        }

        for (task in tasks) {
            val priorityColor = when (task.priority) {
                "HIGH" -> Color.parseColor("#FFCDD2")   // light red
                "MEDIUM" -> Color.parseColor("#FFF9C4") // light yellow
                else -> Color.parseColor("#E3F2FD")     // light blue
            }

            val card = CardView(this).apply {
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

            // Title row with priority badge
            val titleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titleTv = TextView(this).apply {
                text = "${if (task.status == "COMPLETED") "✅ " else "⏳ "}${task.title}"
                textSize = 18f
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val priorityBadge = TextView(this).apply {
                text = task.priority
                textSize = 11f
                setTextColor(Color.DKGRAY)
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 8, 16, 8)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(priorityColor)
                    cornerRadius = 24f
                }
            }

            titleRow.addView(titleTv)
            titleRow.addView(priorityBadge)

            val detailsTv = TextView(this).apply {
                text = "📅 ${task.date}  •  ⏰ ${task.time}"
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 0)
            }

            // Show description preview if available
            if (task.description.isNotEmpty()) {
                val descTv = TextView(this).apply {
                    text = task.description
                    textSize = 13f
                    setTextColor(Color.GRAY)
                    setPadding(0, 4, 0, 0)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                layout.addView(titleRow)
                layout.addView(detailsTv)
                layout.addView(descTv)
            } else {
                layout.addView(titleRow)
                layout.addView(detailsTv)
            }

            card.addView(layout)
            taskContainer.addView(card)
        }
    }

    private fun showTaskOptions(task: Task) {
        val statusText = if (task.status == "PENDING") "Mark as Done" else "Mark as Pending"
        val descriptionText = if (task.description.isEmpty()) "No description." else task.description

        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setMessage(
                "Priority: ${task.priority}\n" +
                "Due: ${task.date} at ${task.time}\n\n" +
                "Description: $descriptionText"
            )
            .setNeutralButton("Back") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Delete") { _, _ ->
                AlertDialog.Builder(this)
                    .setMessage("Delete \"${task.title}\"?")
                    .setPositiveButton("Delete") { _, _ ->
                        taskManager.deleteTask(task.id)
                        refreshTasks()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setPositiveButton(statusText) { _, _ ->
                val newStatus = if (task.status == "PENDING") "COMPLETED" else "PENDING"
                taskManager.updateTask(task.copy(status = newStatus))
                refreshTasks()
            }
            .show()
    }
}
