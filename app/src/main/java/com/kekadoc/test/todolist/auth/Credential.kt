package com.kekadoc.test.todolist.auth

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Credential(
    val login: String,
    val password: String
): Parcelable