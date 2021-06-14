package com.kekadoc.test.todolist.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

interface Navigation {
    fun navigate(destination: Int, data: Bundle = Bundle.EMPTY)
}

fun Fragment.tryNavigate(destination: Int, data: Bundle = Bundle.EMPTY): Boolean {
    try {
        findNavController().navigate(destination, data)
    } catch (e: Throwable) {
        val activity = activity ?: false
        if (activity !is Navigation) return false
        activity.navigate(destination, data)
        return true
    }
    return false
}