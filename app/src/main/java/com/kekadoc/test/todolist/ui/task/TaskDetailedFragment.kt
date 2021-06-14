package com.kekadoc.test.todolist.ui.task

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import coil.load
import coil.request.ImageRequest
import com.kekadoc.test.todolist.R
import com.kekadoc.test.todolist.RepoViewModel
import com.kekadoc.test.todolist.databinding.FragmentTaskDetailedBinding
import com.kekadoc.test.todolist.isSuccess
import com.kekadoc.test.todolist.repository.Task
import com.kekadoc.test.todolist.repository.exist
import com.kekadoc.test.todolist.repository.uniqueName
import com.kekadoc.test.todolist.ui.tryNavigate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for TaskDetailedFragment
 * @see TaskDetailedFragment
 */
class TaskDetailViewModel : ViewModel() {

    companion object {
        private const val TAG: String = "TaskDetailViewModel-TAG"
    }

    private val _targetTask = MutableStateFlow(Task())
    val targetTask = _targetTask.asStateFlow()

    private val _alteredTask = MutableStateFlow(_targetTask.value.copy())
    val alteredTask = _alteredTask.asStateFlow()

    fun notifyTarget(task: Task) {
        _targetTask.value = task
        _alteredTask.value = task.copy()
    }

    fun notifyAltered(task: Task) {
        _alteredTask.value = task
    }

    fun notifyAltered(task: (Task) -> Task) {
        notifyAltered(task(_alteredTask.value))
    }
    fun notifyTarget(task: (Task) -> Task) {
        notifyTarget(task(_targetTask.value))
    }

    fun clearAltered() {
        notifyAltered(_targetTask.value)
    }

}

/**
 * Fragment for additional information about task
 * Use [createInputData] in order to put task
 * The fragment will not be shown without task
 */
class TaskDetailedFragment : DialogFragment() {

    companion object {

        private const val TAG: String = "TaskDetailedFrag-TAG"

        private const val KEY_INPUT_TASK_ID = "InputTask"

        fun createInputData(taskId: Long): Bundle = Bundle(1).apply {
            putLong(KEY_INPUT_TASK_ID, taskId)
        }
        fun parseInputData(data: Bundle?): Long? {
            return data?.getLong(KEY_INPUT_TASK_ID)
        }

        private fun getAlteredImageName(name: String): String {
            return "altered_$name"
        }
        private fun getImageName(task: Task): String {
            return task.uniqueName
        }

    }

    private val repoViewModel by activityViewModels<RepoViewModel>()
    private val viewModel by viewModels<TaskDetailViewModel>()

    /**
     * Get image from storage
     */
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val imageLoader = ImageLoader(requireContext())
        val request = ImageRequest.Builder(requireContext())
            .data(uri)
            .lifecycle(viewLifecycleOwner)
            .target { drawable ->
                val task = viewModel.alteredTask.value
                val bitmapImage = drawable.toBitmap()
                val imageName = getAlteredImageName(task.uniqueName)
                repoViewModel.imageStorage.addImage(bitmap = bitmapImage, imageName, true)
                viewModel.notifyAltered {
                    it.copy(image = imageName)
                }
            }
            .build()
        val disposable = imageLoader.enqueue(request)
    }

    private lateinit var binding: FragmentTaskDetailedBinding

    private fun notifyApplyButton() {
        val item = binding.toolbar.menu.findItem(R.id.menu_item_task_detailed_save)
        val targetTask = viewModel.targetTask.value
        val alteredTask = viewModel.alteredTask.value
        item.isEnabled = targetTask != alteredTask

        if (item.isEnabled) item.icon.alpha = 255
        else item.icon.alpha = 130
    }
    private fun saveChange() {
        val targetTask = viewModel.targetTask.value
        val alteredTask = viewModel.alteredTask.value
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val newTask = alteredTask.let {
                    if (it.image != null) {
                        return@let it.copy(image = getImageName(targetTask))
                    } else return@let it
                }
                if (!newTask.exist) {
                    notifyApplyButton()
                    throw IllegalStateException()
                }
                repoViewModel.update(newTask) {
                    if (it.isSuccess && alteredTask.image != null && newTask.image != null) {
                        repoViewModel.imageStorage.renameImage(alteredTask.image, newTask.image)
                    }
                    if(it.isSuccess && newTask.image == null) {
                        repoViewModel.imageStorage.removeImage(getImageName(newTask))
                    }
                    viewModel.notifyTarget(newTask)
                    notifyApplyButton()
                }
            }catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        R.string.fragment_task_detailed_message_error_save,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Clear temporary data
     */
    private fun clearAltered() {
        val alteredImage = viewModel.alteredTask.value.image
        viewModel.clearAltered()
        if (viewModel.targetTask.value.image == alteredImage) return
        if (alteredImage != null) {
            repoViewModel.imageStorage.removeImage(alteredImage)
        }
    }

    private fun updateTaskUI(task: Task) {
        if (!binding.editTextName.isFocused) binding.editTextName.setText(task.name)
        if (!binding.editTextDescription.isFocused) binding.editTextDescription.setText(task.description)
        updateImage(task)
    }
    private fun updateImage(task: Task) {
        if (task.image == null) {
            binding.cardViewImage.visibility = View.GONE
            binding.imageView.setImageDrawable(null)
        } else {
            val file = repoViewModel.imageStorage.getImage(task.image)
            binding.imageView.load(file) {
                lifecycle(viewLifecycleOwner)
                listener(
                    onStart = {
                        binding.cardViewImage.visibility = View.VISIBLE
                    },
                    onError = { _, fail ->
                        binding.cardViewImage.visibility = View.GONE
                    },
                    onCancel = {
                        binding.cardViewImage.visibility = View.GONE
                    },
                    onSuccess = { _, meta ->
                        binding.cardViewImage.visibility = View.VISIBLE
                    }
                )
            }
        }
    }

    /**
     * Close this dialog
     */
    private fun close() {
        try {
            findNavController().popBackStack()
        }catch (e: IllegalStateException) {
            val navigate = tryNavigate(R.id.destination_all_tasks)
            if (!navigate) dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        //Parse input data, if empty then close dialog
        parseInputData(arguments)?.let {
            lifecycleScope.launch {
                repoViewModel.get(it)?.let {
                    viewModel.notifyTarget(it)
                }
            }
        } ?: close()
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTaskDetailedBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.alteredTask.onEach {
            updateTaskUI(it)
            notifyApplyButton()
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        setFragmentResultListener(TaskOptionsDialogFragment.RC_ACTION) { _, result ->
            when(TaskOptionsDialogFragment.parseResult(result)) {
                TaskOption.IMAGE_CHANGE -> getImage.launch("image/*")
                TaskOption.IMAGE_REMOVE -> viewModel.notifyAltered { it.copy(image = null) }
                TaskOption.DELETE -> {
                    repoViewModel.delete(viewModel.targetTask.value) {
                        Log.e(TAG, "onViewCreated: $it ${findNavController().currentDestination}")
                        close()
                    }
                }
            }
        }

        binding.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when(it.itemId) {
                R.id.menu_item_task_detailed_additional -> {
                    tryNavigate(R.id.action_destination_task_detailed_to_taskOptionsDialogFragment)
                    true
                }
                R.id.menu_item_task_detailed_save -> {
                    saveChange()
                    true
                }
                else -> false
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            close()
        }

        binding.editTextName.addTextChangedListener { editable ->
            val text = editable?.toString().let { if (it.isNullOrEmpty()) null else it }
            viewModel.notifyAltered(viewModel.alteredTask.value.copy(name = text))
        }
        binding.editTextDescription.addTextChangedListener { editable ->
        val text = editable?.toString().let { if (it.isNullOrEmpty()) null else it }
            viewModel.notifyAltered(viewModel.alteredTask.value.copy(description = text))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        clearAltered()
    }

}