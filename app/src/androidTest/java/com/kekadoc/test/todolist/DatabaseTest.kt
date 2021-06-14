package com.kekadoc.test.todolist

import android.content.Context
import androidx.room.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kekadoc.test.todolist.repository.AppDatabase
import com.kekadoc.test.todolist.repository.Task
import com.kekadoc.test.todolist.repository.TaskDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    companion object {
        private const val TAG: String = "DatabaseTest-TAG"
    }
    private lateinit var taskDao: TaskDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        taskDao = db.tasksDao()
    }
    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun getAll() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
            Task(id = 1),
            Task(id = 2),
            Task(id = 3),
            Task(id = 4),
            Task(id = 5),
        )
        taskDao.insertAll(tasks)
        val tasksFromBd = taskDao.getAll()
        assert(tasksFromBd == tasks)
    }
    @Test
    fun getAllById() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
            Task(id = 1),
            Task(id = 2),
            Task(id = 3),
            Task(id = 4),
            Task(id = 5),
        )
        val findId = arrayListOf<Long>(2, 4, 1)
        val byId = tasks.filter {
            findId.contains(it.id)
        }

        taskDao.insertAll(tasks)
        val tasksFromBd = taskDao.getAll(findId)
        assert(tasksFromBd == byId)
    }
    @Test
    fun getById() = runBlocking {
        db.clearAllTables()

        val task = Task(1)
        taskDao.insert(task)
        val taskFromDb = taskDao.get(task.id)
        assert(task == taskFromDb)
    }
    @Test
    fun update() = runBlocking {
        db.clearAllTables()

        val task = Task(1)
        taskDao.insert(task)
        taskDao.update(task.copy(name = "Work"))
        val taskFromDb = taskDao.get(task.id)
        assert(taskFromDb != null)
        assert(task != taskFromDb)
        assert(taskFromDb!!.name == "Work")
    }
    @Test
    fun count() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
            Task(id = 1),
            Task(id = 2),
            Task(id = 3),
            Task(id = 4),
            Task(id = 5),
        )
        taskDao.insertAll(tasks)
        val count = taskDao.getCount()
        assert(count == tasks.size.toLong())
    }
    @Test
    fun insertAll() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
            Task(id = 1),
            Task(id = 2),
            Task(id = 3),
            Task(id = 4),
            Task(id = 5),
        )
        taskDao.insertAll(tasks)
        val tasksFromBd = taskDao.getAll()
        assert(tasksFromBd == tasks)
    }
    @Test
    fun insert() = runBlocking {
        db.clearAllTables()

        val task = Task(1)
        taskDao.insert(task)
        val taskFromDb = taskDao.get(task.id)
        assert(taskFromDb != null)
        assert(task == taskFromDb)
    }
    @Test
    fun delete() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
            Task(id = 1),
            Task(id = 2),
            Task(id = 3),
            Task(id = 4),
            Task(id = 5),
        )
        val delete = tasks[1]
        taskDao.insertAll(tasks)
        taskDao.delete(delete)
        val tasksFromBd = taskDao.getAll()
        assert(!tasksFromBd.contains(delete))
    }
    @Test
    fun deleteById() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
                Task(id = 1),
                Task(id = 2),
                Task(id = 3),
                Task(id = 4),
                Task(id = 5),
            )
            val delete = tasks[1]
            taskDao.insertAll(tasks)
            taskDao.delete(delete.id)
            val tasksFromBd = taskDao.getAll()
            assert(!tasksFromBd.contains(delete))
    }
    @Test
    fun getFlowById() = runBlocking {
        db.clearAllTables()

        val task = Task(id = 1)
        val expected = mutableListOf<Task>()
        val received = mutableListOf<Task>()
        taskDao.insert(task)
        expected.add(task)
        launch {
            repeat(4) {
                delay(300)
                val newTask = task.copy(name = "Task#$it")
                taskDao.update(newTask)
                expected.add(newTask)
            }
        }
        val flow = taskDao.getFlow(task.id).take(5).collect {
            received.add(it)
        }

        assert(expected == received) {
            "expected: ${expected.size} $expected \n received: ${received.size} $received"
        }
    }
    @Test
    fun getAllFlow() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
            Task(id = 1),
            Task(id = 2),
            Task(id = 3),
            Task(id = 4),
            Task(id = 5),
        )
        val expected = tasks.toMutableList().apply {
            remove(tasks[1])
        }
        var received = listOf<Task>()
        taskDao.insertAll(tasks)
        launch {
            delay(300)
            taskDao.delete(tasks[1])
        }
        val flow = taskDao.getAllFlow().take(2).onEach {
            received = it
        }.collect()

        assert(expected == received) {
            "\nexpected: ${expected.size} $expected \nreceived: ${received.size} $received"
        }
    }
    @Test
    fun getAllPaged() = runBlocking {
        db.clearAllTables()

        val tasks = listOf(
            Task(id = 1),
            Task(id = 2),
            Task(id = 3),
            Task(id = 4),
            Task(id = 5),
        )
        taskDao.insertAll(tasks)
        val paged = taskDao.getAllPaged()
    }

}