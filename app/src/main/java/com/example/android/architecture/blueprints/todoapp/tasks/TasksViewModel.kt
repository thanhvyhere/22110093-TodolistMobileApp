package com.example.android.architecture.blueprints.todoapp.tasks

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import com.example.android.architecture.blueprints.todoapp.Event
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Result.Success
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.DefaultTasksRepository
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import kotlinx.coroutines.launch
import androidx.lifecycle.LiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksViewModel(application: Application) : AndroidViewModel(application) {

    private val tasksRepository = DefaultTasksRepository.getRepository(application)

    private val _forceUpdate = MutableLiveData<Boolean>(false)

    private val _items = MutableLiveData<List<Task>>()
    val items: LiveData<List<Task>> = _items

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _currentFilteringLabel = MutableLiveData<Int>()
    val currentFilteringLabel: LiveData<Int> = _currentFilteringLabel

    private val _noTasksLabel = MutableLiveData<Int>()
    val noTasksLabel: LiveData<Int> = _noTasksLabel

    private val _noTaskIconRes = MutableLiveData<Int>().apply {
        value = R.drawable.ic_no_tasks
    }
    val noTaskIconRes: LiveData<Int> = _noTaskIconRes

    private val _tasksAddViewVisible = MutableLiveData<Boolean>()
    val tasksAddViewVisible: LiveData<Boolean> = _tasksAddViewVisible

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarText: LiveData<Event<Int>> = _snackbarText

    private var currentFiltering = TasksFilterType.ALL_TASKS

    private val isDataLoadingError = MutableLiveData<Boolean>()

    private val _openTaskEvent = MutableLiveData<Event<String>>()
    val openTaskEvent: LiveData<Event<String>> = _openTaskEvent

    private val _newTaskEvent = MutableLiveData<Event<Unit>>()
    val newTaskEvent: LiveData<Event<Unit>> = _newTaskEvent

    private var resultMessageShown: Boolean = false

    val empty: LiveData<Boolean> = _items.map { it.isEmpty() }

    init {
        setFiltering(TasksFilterType.ALL_TASKS)
        loadTasks(true)
    }

    private fun showSnackbarMessage(message: Int) {
        _snackbarText.value = Event(message)
    }

    fun setFiltering(requestType: TasksFilterType) {
        currentFiltering = requestType
        when (requestType) {
            TasksFilterType.ALL_TASKS -> {
                setFilter(R.string.label_all, R.string.no_tasks_all, R.drawable.logo_no_fill, true)
            }
            TasksFilterType.ACTIVE_TASKS -> {
                setFilter(R.string.label_active, R.string.no_tasks_active, R.drawable.ic_check_circle_96dp, false)
            }
            TasksFilterType.COMPLETED_TASKS -> {
                setFilter(R.string.label_completed, R.string.no_tasks_completed, R.drawable.ic_verified_user_96dp, false)
            }
        }
        loadTasks(false)
    }

    private fun setFilter(
        @StringRes filteringLabelString: Int,
        @StringRes noTasksLabelString: Int,
        @DrawableRes noTaskIconDrawable: Int,
        tasksAddVisible: Boolean
    ) {
        _currentFilteringLabel.postValue(filteringLabelString)
        _noTasksLabel.postValue(noTasksLabelString)
        _noTaskIconRes.postValue(noTaskIconDrawable)
        _tasksAddViewVisible.postValue(tasksAddVisible)
    }

    fun loadTasks(forceUpdate: Boolean) {
        viewModelScope.launch {
            _dataLoading.value = true
            if (forceUpdate) {
                tasksRepository.refreshTasks()
            }

            val result = tasksRepository.getTasks()
            if (result is Success) {
                _items.value = filterTasksList(result.data)
                isDataLoadingError.value = false
            } else {
                _items.value = emptyList()
                isDataLoadingError.value = true
            }

            _dataLoading.value = false
        }
    }

    fun refresh() {
        loadTasks(true)
    }

    fun openTask(taskId: String) {
        _openTaskEvent.value = Event(taskId)
    }

    fun completeTask(task: Task, completed: Boolean) {
        viewModelScope.launch {
            if (completed) {
                tasksRepository.completeTask(task)
                _snackbarText.value = Event(R.string.task_marked_complete)
            } else {
                tasksRepository.activateTask(task)
                _snackbarText.value = Event(R.string.task_marked_active)
            }
            loadTasks(false)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            tasksRepository.clearCompletedTasks()
            showSnackbarMessage(R.string.completed_tasks_cleared)
            loadTasks(false)
        }
    }

    fun showEditResultMessage(userMessage: Int) {
        if (userMessage == ADD_EDIT_RESULT_OK) {
            showSnackbarMessage(R.string.successfully_saved_task_message)
        } else if (userMessage == EDIT_RESULT_OK) {
            showSnackbarMessage(R.string.successfully_updated_task_message)
        } else if (userMessage == DELETE_RESULT_OK) {
            showSnackbarMessage(R.string.successfully_deleted_task_message)
        }
    }

    private fun filterTasksList(tasks: List<Task>): List<Task> {
        return tasks.filter { task ->
            when (currentFiltering) {
                TasksFilterType.ACTIVE_TASKS -> task.isActive
                TasksFilterType.COMPLETED_TASKS -> task.isCompleted
                else -> true
            }
        }
    }

    fun sortTasksByName() {
        _items.value = _items.value?.sortedBy { it.title.lowercase() }
    }
    fun sortTasksByDate() {
        _items.value = _items.value?.sortedBy { it.dueDateTime }
    }
}
