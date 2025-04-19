package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class TaskListItem {
    data class Header(val date: String) : TaskListItem()
    data class TaskItem(val task: Task) : TaskListItem()
}
