package com.example.studenttask

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskManager(context: Context) {
    private val prefs = context.getSharedPreferences("Tasks", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getTasks(): MutableList<Task> {
        val json = prefs.getString("tasks_list", "[]")
        val type = object : TypeToken<MutableList<Task>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()
    }

    fun saveTasks(tasks: List<Task>) {
        val json = gson.toJson(tasks)
        prefs.edit().putString("tasks_list", json).apply()
    }

    fun addTask(task: Task) {
        val tasks = getTasks()
        tasks.add(task)
        saveTasks(tasks)
    }

    fun updateTask(task: Task) {
        val tasks = getTasks()
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
            saveTasks(tasks)
        }
    }

    fun deleteTask(id: Long) {
        val tasks = getTasks().filter { it.id != id }
        saveTasks(tasks)
    }

    // Fixed: only removes the tasks list, not all shared prefs keys
    fun clearAllTasks() {
        prefs.edit().putString("tasks_list", "[]").apply()
    }

    // Filter by status; null returns all tasks
    fun getTasksByStatus(status: String?): MutableList<Task> {
        val tasks = getTasks()
        return if (status == null) tasks else tasks.filter { it.task_status() == status }.toMutableList()
    }

    // Sort tasks by due date (earliest first), then by priority weight
    fun getSortedByDate(tasks: List<Task>): List<Task> {
        return tasks.sortedWith(compareBy({ parseDateToLong(it.date, it.time) }, { priorityWeight(it.priority) }))
    }

    // Sort tasks by priority (HIGH first), then by date
    fun getSortedByPriority(tasks: List<Task>): List<Task> {
        return tasks.sortedWith(compareBy({ priorityWeight(it.priority) }, { parseDateToLong(it.date, it.time) }))
    }

    fun getPendingCount(): Int = getTasks().count { it.status == "PENDING" }

    fun getCompletedCount(): Int = getTasks().count { it.status == "COMPLETED" }

    private fun priorityWeight(priority: String): Int = when (priority) {
        "HIGH" -> 0
        "MEDIUM" -> 1
        "LOW" -> 2
        else -> 1
    }

    private fun parseDateToLong(date: String, time: String): Long {
        return try {
            val parts = date.split("/")
            val timeParts = time.split(":")
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val year = parts[2].toInt()
            val hour = if (timeParts.size == 2) timeParts[0].toInt() else 0
            val min = if (timeParts.size == 2) timeParts[1].toInt() else 0
            val cal = java.util.Calendar.getInstance()
            cal.set(year, month, day, hour, min, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    private fun Task.task_status() = this.status
}
