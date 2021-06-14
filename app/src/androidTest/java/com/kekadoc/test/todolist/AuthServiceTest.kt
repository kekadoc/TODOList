package com.kekadoc.test.todolist

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kekadoc.test.todolist.auth.AuthService
import com.kekadoc.test.todolist.auth.Credential
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthServiceTest {

    private lateinit var authService: AuthService

    @Before
    fun create() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        authService = AuthService.getInstance(context, "TestCredentials")
    }
    @After
    fun close() {
        authService.release()
    }

    @Test
    fun setCredential() {
        val credential = Credential("Oleg", "1234")
        authService.setCredential(credential)
        assertThat(authService.credential.value).isEqualTo(credential)
    }
    @Test
    fun getCredentialFlow() = runBlocking {
        authService.setCredential(null)
        val expected = mapOf(
            0 to null,
            1 to Credential("Oleg_0", "1234"),
            2 to null,
            3 to Credential("Oleg_2", "1234"),
            4 to Credential("Oleg_3", "1234")
        )

        (1..4).forEach {
            authService.setCredential(expected[it])
        }

        var saved: Credential? = null
        authService.credential.take(1).collect {
            Log.e("TAG", "getCredentialFlow: $it")
            saved = it
        }
        assertThat(saved).isEqualTo(expected[4])
    }

}