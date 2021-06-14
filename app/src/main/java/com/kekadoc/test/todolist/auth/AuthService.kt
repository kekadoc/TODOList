package com.kekadoc.test.todolist.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for authentication
 *
 * @param context Context
 * @param name File name for save
 */
class AuthService private constructor(context: Context, name: String = CREDENTIALS_SPACE) {

    companion object {
        @Volatile private var instance: AuthService? = null
        fun getInstance(context: Context, name: String = CREDENTIALS_SPACE): AuthService {
            return instance ?: synchronized(this) {
                instance ?: AuthService(context, name).also { instance = it }
            }
        }
        private const val CREDENTIALS_SPACE = "credentials"
        private const val KEY_CREDENTIAL_LOGIN = "login"
        private const val KEY_CREDENTIAL_PASSWORD = "password"
    }

    private val preference = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == KEY_CREDENTIAL_LOGIN) {
            val credential = getCredential(sharedPreferences)
            _credential.value = credential
        }
    }

    /**
     * Current auth
     */
    private val _credential = MutableStateFlow<Credential?>(getCredential())
    /**
     * Current auth
     */
    val credential = _credential.asStateFlow()

    init {
        start()
    }

    /**
     * Check if user logged in
     */
    fun isAuth() = _credential.value != null

    private fun start() {
        preference.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Remove listener
     */
    fun release() {
        preference.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Authentication
     */
    fun setCredential(credentials: Credential?) {
        preference.edit {
            putString(KEY_CREDENTIAL_LOGIN, credentials?.login)
            putString(KEY_CREDENTIAL_PASSWORD, credentials?.password)
        }
    }

    /**
     * Try load last auth
     */
    private fun getCredential(): Credential? {
        return getCredential(preference)
    }

    /**
     * Try load last auth from current sharedPreferences
     */
    private fun getCredential(sharedPreferences: SharedPreferences): Credential? {
        val login = sharedPreferences.getString(KEY_CREDENTIAL_LOGIN, null)
        val password = sharedPreferences.getString(KEY_CREDENTIAL_PASSWORD, null)
        if (login == null || password == null) return null
        return Credential(login, password)
    }

}