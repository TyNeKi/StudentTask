package com.example.studenttask

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class SaveTask : AppCompatActivity() {

    private lateinit var taskManager: TaskManager
    private var selectedPriority = "MEDIUM"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_task)

        taskManager = TaskManager(this)

        val etTitle = findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = findViewById<TextInputEditText>(R.id.etDescription)
        val tvDate = findViewById<TextView>(R.id.tvSelectedDate)
        val tvTime = findViewById<TextView>(R.id.tvSelectedTime)
        val dateBtn = findViewById<androidx.cardview.widget.CardView>(R.id.datePickerBtn)
        val timeBtn = findViewById<androidx.cardview.widget.CardView>(R.id.timePickerBtn)
        val btnSave = findViewById<Button>(R.id.btnSaveTask)
        val backBtn = findViewById<Button>(R.id.backBtn)

        // Priority radio buttons — expects RadioGroup with id priorityGroup
        // and RadioButtons: rbLow, rbMedium, rbHigh in layout
        val priorityGroup = findViewById<RadioGroup>(R.id.priorityGroup)
        priorityGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPriority = when (checkedId) {
                R.id.rbLow -> "LOW"
                R.id.rbHigh -> "HIGH"
                else -> "MEDIUM"
            }
        }
        // Default selection
        priorityGroup.check(R.id.rbMedium)

        backBtn.setOnClickListener { finish() }

        dateBtn.setOnClickListener {
            val c = Calendar.getInstance()
            val dialog = DatePickerDialog(
                this,
                { _, y, m, d -> tvDate.text = "$d/${m + 1}/$y" },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
            // Prevent picking past dates
            dialog.datePicker.minDate = System.currentTimeMillis() - 1000
            dialog.show()
        }

        timeBtn.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, h, m ->
                    tvTime.text = String.format("%02d:%02d", h, m)
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                false
            ).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()

            when {
                title.isEmpty() -> {
                    etTitle.error = "Title is required"
                    etTitle.requestFocus()
                }
                tvDate.text == "Select Date" -> {
                    Toast.makeText(this, "Please select a due date.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Default time to 08:00 if user skipped it
                    val time = if (tvTime.text == "Select Time") "08:00" else tvTime.text.toString()

                    val newTask = Task(
                        id = System.currentTimeMillis(),
                        title = title,
                        description = description,
                        date = tvDate.text.toString(),
                        time = time,
                        status = "PENDING",
                        priority = selectedPriority
                    )
                    taskManager.addTask(newTask)
                    scheduleNotification(newTask)
                    Toast.makeText(this, "Task saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun scheduleNotification(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }

        val triggerTime = parseTriggerTime(task.date, task.time)

        // Don't schedule if the time is already in the past
        if (triggerTime <= System.currentTimeMillis()) {
            Toast.makeText(this, "Note: Due time is in the past — no reminder set.", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(this, TaskReminderReceiver::class.java).apply {
            putExtra("title", task.title)
            putExtra("taskId", task.id)
        }

        // Use task.id as request code so each task gets its own unique alarm
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    private fun parseTriggerTime(date: String, time: String): Long {
        return try {
            val dateParts = date.split("/")
            val timeParts = time.split(":")
            val cal = Calendar.getInstance()
            cal.set(
                dateParts[2].toInt(),       // year
                dateParts[1].toInt() - 1,   // month (0-indexed)
                dateParts[0].toInt(),        // day
                timeParts[0].toInt(),        // hour
                timeParts[1].toInt(),        // minute
                0
            )
            cal.timeInMillis
        } catch (e: Exception) {
            // Fallback: 5 seconds from now (safe default)
            System.currentTimeMillis() + 5000
        }
    }
}
