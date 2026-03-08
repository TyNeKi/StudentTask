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
            saveTasks(tasks) // The critical fix for the toggle!
        }
    }

    fun deleteTask(id: Long) {
        val tasks = getTasks().filter { it.id != id }
        saveTasks(tasks)
    }

    fun clearAllTasks() = prefs.edit().clear().apply()
}