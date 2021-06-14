package com.kekadoc.test.todolist

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.kekadoc.test.todolist.databinding.ActivityMainBinding
import com.kekadoc.test.todolist.ui.Navigation
import kotlinx.coroutines.flow.*

class MainActivity : AppCompatActivity(), Navigation {

    companion object {
        private const val TAG: String = "MainActivity-TAG"
    }

    private val navController by lazy {
        findNavController(R.id.nav_host_fragment_activity_main)
    }

    private val repoViewModel by viewModels<RepoViewModel>()

    private lateinit var binding: ActivityMainBinding

    override fun navigate(destination: Int, data: Bundle) {
        navController.navigate(destination, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: $savedInstanceState")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            repoViewModel.credential.onEach {
                if (it == null) navigate(R.id.destination_login)
                else navigate(R.id.destination_all_tasks)
            }.launchIn(lifecycleScope)
        }

    }

}