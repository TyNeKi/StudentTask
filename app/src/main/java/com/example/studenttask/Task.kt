package com.example.studenttask

import java.io.Serializable

data class Task(
    val id: Long,
    val title: String,
    val description: String = "",
    val date: String,
    val time: String,
    val status: String = "PENDING"
) : Serializable