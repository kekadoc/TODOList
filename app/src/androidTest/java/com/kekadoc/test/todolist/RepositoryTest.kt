package com.kekadoc.test.todolist

import android.content.Context
import android.util.Log
import androidx.room.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kekadoc.test.todolist.auth.Credential
import com.kekadoc.test.todolist.repository.*
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG: String = "Test-TAG"

@RunWith(AndroidJUnit4::class)
class RepositoryTest {

    private lateinit var repository: Repository

    @Before
    fun create() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = Repository.getInstance(context, db)
    }
    @After
    fun close() {}

    @Test
    fun login() {
      //  Log.e(TAG, "login: ${repository.getCredentials()}")
        val credential = Credential("Oleg", "1234")
      //  repository.saveCredentials(credential)
     //   assert(repository.getCredentials() == credential)
    }


}