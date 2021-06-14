package com.kekadoc.test.todolist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import com.kekadoc.test.todolist.auth.AuthService
import com.kekadoc.test.todolist.auth.Credential
import com.kekadoc.test.todolist.repository.ImageStorage
import com.kekadoc.test.todolist.repository.Repository
import com.kekadoc.test.todolist.repository.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

sealed class Result<T> {
    class Error<T>(val fail: Throwable) : Result<T>()
    data class Success<T>(val value: T) : Result<T>()
}

val <T> Result<T>.isSuccess: Boolean
    get() = this is Result.Success

val <T> Result<T>.isFailed: Boolean
    get() = this is Result.Error

typealias Callback<T> = (Result<T>) -> Unit

class RepoViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG: String = "RepoViewModel-TAG"
    }

    val imageStorage = ImageStorage.getInstance(application)
    val repository = Repository.getInstance(application)
    val authService = AuthService.getInstance(application)

    val credential = authService.credential

    fun logIn(credential: Credential) {
        authService.setCredential(credential)
    }
    fun logOut() {
        authService.setCredential(null)
    }

    val flow: Flow<List<Task>> = repository.tasks.getAllFlow()
    val dataSource: DataSource.Factory<Int, Task> = repository.tasks.getAllPaged()

    suspend fun getAll(): List<Task> {
        return repository.tasks.getAll()
    }
    suspend fun getAll(ids: List<Long>): List<Task> {
        return repository.tasks.getAll(ids)
    }
    suspend fun get(id: Long): Task? {
        return repository.tasks.get(id)
    }
    suspend fun getCount(): Long {
        return repository.tasks.getCount()
    }

    fun getFlow(id: Long): Flow<Task> {
        return repository.tasks.getFlow(id)
    }

    fun <T> run(callback: Callback<T>? = null, block: suspend CoroutineScope.() -> T) {
        launch(callback, block)
    }

    fun update(task: Task, callback: Callback<Unit>? = null) {
        launch(callback) {
            repository.tasks.update(task)
        }
    }
    fun insertAll(values: List<Task>, callback: Callback<Unit>? = null) {
        launch(callback) {
            repository.tasks.insertAll(values)
        }
    }
    fun insert(value: Task, callback: Callback<Long>? = null) {
        launch(callback) {
            repository.tasks.insert(value)
        }
    }
    fun delete(task: Task, callback: Callback<Unit>? = null) {
        launch(callback) {
            repository.tasks.delete(task)
            Log.e(TAG, "delete: ${imageStorage.removeImage(task.image)}")
        }
    }

    private fun <T> launch(callback: Callback<T>?, block: suspend CoroutineScope.() -> T) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = block(this)
                withContext(Dispatchers.Main) {
                    callback?.invoke(Result.Success(result))
                }
            }catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    callback?.invoke(Result.Error(e))
                }
            }
        }
    }

}