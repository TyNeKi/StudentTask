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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_task)

        taskManager = TaskManager(this)

        val etTitle = findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = findViewById<TextInputEditText>(R.id.etDescription) // Added this
        val tvDate = findViewById<TextView>(R.id.tvSelectedDate)
        val tvTime = findViewById<TextView>(R.id.tvSelectedTime)
        val dateBtn = findViewById<androidx.cardview.widget.CardView>(R.id.datePickerBtn)
        val timeBtn = findViewById<androidx.cardview.widget.CardView>(R.id.timePickerBtn)
        val btnSave = findViewById<Button>(R.id.btnSaveTask)
        val backBtn = findViewById<Button>(R.id.backBtn)

        backBtn.setOnClickListener { finish() }

        dateBtn.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d -> tvDate.text = "$d/${m + 1}/$y" },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        timeBtn.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m -> tvTime.text = String.format("%02d:%02d", h, m) },
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim() // Get description

            if (title.isNotEmpty() && tvDate.text != "Select Date") {
                val newTask = Task(
                    id = System.currentTimeMillis(),
                    title = title,
                    description = description, // Save description here
                    date = tvDate.text.toString(),
                    time = tvTime.text.toString(),
                    status = "PENDING"
                )
                taskManager.addTask(newTask)
                scheduleNotification(title)
                finish()
            } else {
                Toast.makeText(this, "Please enter a title and date!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleNotification(title: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            return
        }
        val intent = Intent(this, TaskReminderReceiver::class.java).apply { putExtra("title", title) }
        val pendingIntent = PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        // Sets alarm for 5 seconds from now for testing; you can calculate real time based on task date/time
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pendingIntent)
    }
}