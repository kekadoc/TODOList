package com.kekadoc.test.todolist.ui.task

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.kekadoc.test.todolist.R
import com.kekadoc.test.todolist.databinding.FragmentTaskOptionsDialogBinding
import com.kekadoc.test.todolist.ui.tryNavigate

/**
 * Event from dialog
 * @see TaskOptionsDialogFragment
 */
enum class TaskOption {
    IMAGE_CHANGE,
    IMAGE_REMOVE,
    DELETE
}

/**
 * Dialog for additional settings
 *
 * Use [RC_ACTION] for subscribe to events
 *
 * @see androidx.fragment.app.FragmentResultListener
 * @see TaskOption
 */
class TaskOptionsDialogFragment : BottomSheetDialogFragment() {

    companion object {
        private const val TAG: String = "TaskOptionsDialogFragment-TAG"

        const val RC_ACTION = "TaskOptionCode"
        private const val KEY_RESULT_OPTION = "ResultOption"

        fun parseResult(result: Bundle): TaskOption? {
            val optionStr = result.getString(KEY_RESULT_OPTION, null) ?: return null
            return try {
                TaskOption.valueOf(optionStr)
            } catch (error: IllegalArgumentException) {
                null
            }
        }

        private fun createResult(option: TaskOption): Bundle {
            return Bundle(1).apply {
                putString(KEY_RESULT_OPTION, option.toString())
            }
        }

    }

    private lateinit var binding: FragmentTaskOptionsDialogBinding
    private val listener = View.OnClickListener {
        when(it.id) {
            R.id.button_changeImage -> {
                close()
                setFragmentResult(RC_ACTION, createResult(TaskOption.IMAGE_CHANGE))
            }
            R.id.button_removeImage -> {
                close()
                setFragmentResult(RC_ACTION, createResult(TaskOption.IMAGE_REMOVE))
            }
            R.id.button_delete -> {
                close()
                setFragmentResult(RC_ACTION, createResult(TaskOption.DELETE))
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
            val navigate = tryNavigate(R.id.destination_task_detailed)
            if (!navigate) dismiss()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskOptionsDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.buttonChangeImage.setOnClickListener(listener)
        binding.buttonRemoveImage.setOnClickListener(listener)
        binding.buttonDelete.setOnClickListener(listener)
    }

}