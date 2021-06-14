package com.kekadoc.test.todolist.ui.tasks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.kekadoc.test.todolist.*
import com.kekadoc.test.todolist.databinding.FragmentAllTasksBinding
import com.kekadoc.test.todolist.databinding.TaskHeaderViewBinding
import com.kekadoc.test.todolist.databinding.TaskPreviewBinding
import com.kekadoc.test.todolist.repository.Repository
import com.kekadoc.test.todolist.repository.Task
import com.kekadoc.test.todolist.ui.task.TaskDetailedFragment
import com.kekadoc.test.todolist.ui.tryNavigate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * View
 */
class AllTasksViewModel(repository: Repository) : ViewModel() {

    private val dataSourceFactory = repository.tasks.getAllPaged()
    private val config = PagingConfig(
        pageSize = 5,
        enablePlaceholders = false
    )

    val pager = Pager(
        config = config,
        initialKey = 0,
        pagingSourceFactory = dataSourceFactory.asPagingSourceFactory()
    )

}
class AllTasksViewModelFactory(val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (AllTasksViewModel::class.java.isAssignableFrom(modelClass)) {
            val constructor = modelClass.getConstructor(Repository::class.java)
            return constructor.newInstance(repository)
        } else throw NotImplementedError()
    }
}

class AllTaskFragment : Fragment() {

    companion object {
        private const val TAG: String = "AllTaskFragment-TAG"
    }

    val timeFormat by lazy {
        SimpleDateFormat("dd-MM-yyyy hh:mm", getLocale(requireContext()))
    }

    private val repoViewModel by activityViewModels<RepoViewModel>()
    private val viewModel by viewModels<AllTasksViewModel>(
        factoryProducer = {
            AllTasksViewModelFactory(repoViewModel.repository)
        }
    )

    private val taskViewActionCallback = object : TaskViewActionCallback {
        override fun onLongClick(holder: VH.Value): Boolean {
            return true
        }
        override fun onClick(holder: VH.Value) {
            showTaskDetailsDialog(holder.item.task.id)
        }
        override fun onChecked(holder: VH.Value, checked: Boolean) {
            val time = if (checked) System.currentTimeMillis() else null
            repoViewModel.update(holder.item.task.copy(isComplete = checked, timeOfCompletion = time)) {
                if (it.isSuccess) binding.recyclerView.adapter!!.notifyItemChanged(holder.bindingAdapterPosition)
            }
        }
        override fun onLoadImage(imageView: ImageView, imageName: String) {
            imageView.load(repoViewModel.imageStorage.getImage(imageName))
        }
        override fun onTime(value: Long): String {
            return timeFormat.format(Date(value))
        }
    }
    private val taskSwipeCallback = object : SwipeCallback {
        override fun onSwipeLeft(viewHolder: VH.Value) {
            repoViewModel.delete(viewHolder.item.task) {
                if (it.isFailed)
                    this@AllTaskFragment.adapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
            }
        }
        override fun onSwipeRight(viewHolder: VH.Value) {
            throw NotImplementedError()
        }
    }

    private lateinit var binding: FragmentAllTasksBinding
    private val adapter = Adapter(taskViewActionCallback)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAllTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when(it.itemId) {
                R.id.menu_item_logout -> {
                    repoViewModel.logOut()
                    true
                }
                else -> false
            }
        }
        binding.recyclerView.apply {
            addItemDecoration(Decorator())
            adapter = this@AllTaskFragment.adapter
            ItemTouchHelper(SwipeToDeleteCallback(taskSwipeCallback)).attachToRecyclerView(this)
        }
        binding.buttonAdd.setOnClickListener {
            repoViewModel.insert(Task()) {
                if (it.isSuccess) showTaskDetailsDialog((it as Result.Success).value)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.pager.flow.onEach { pagingData ->
                val data = pagingData.map { Item.Value(it) }.insertSeparators { before: Item.Value?, next: Item.Value? ->
                    if ((before == null || !before.task.isComplete) && (next != null && next.task.isComplete )) {
                        return@insertSeparators Item.Header(getString(R.string.fragment_all_tasks_header_done))
                    }
                    if ((before == null || before.task.isComplete) && (next != null && !next.task.isComplete )) {
                        return@insertSeparators Item.Header(getString(R.string.fragment_all_tasks_header_new))
                    }
                    return@insertSeparators null
                }
                adapter.submitData(viewLifecycleOwner.lifecycle, data)
            }.collect()
        }

        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                when(loadStates.refresh) {
                    is LoadState.Loading -> binding.indicator.show()
                    else -> binding.indicator.hide()
                }
            }
        }

    }

    private fun showTaskDetailsDialog(taskId: Long) {
        tryNavigate(R.id.destination_task_detailed, TaskDetailedFragment.createInputData(taskId))
    }

    private sealed class Item {

        abstract val id: Long
        abstract fun isEquals(item: Item): Boolean

        data class Header(val text: String): Item() {
            override val id: Long
                get() = Long.MIN_VALUE

            override fun isEquals(item: Item): Boolean {
                if (item !is Item.Header) return false
                return text == item.text
            }
        }
        data class Value(val task: Task): Item() {
            override val id: Long
                get() = task.id

            override fun isEquals(item: Item): Boolean {
                if (item !is Value) return false
                return task.timeOfCreation == item.task.timeOfCreation
                        && task.timeOfCompletion == item.task.timeOfCompletion
                        && task.isComplete == item.task.isComplete
                        && task.name == item.task.name
                        && task.image == item.task.image
                        && task.description == item.task.description
            }
        }

    }

    private interface TaskViewActionCallback {
        fun onLongClick(holder: VH.Value): Boolean
        fun onClick(holder: VH.Value)
        fun onChecked(holder: VH.Value, checked: Boolean)
        fun onLoadImage(imageView: ImageView, imageName: String)
        fun onTime(value: Long): String
    }
    private interface SwipeCallback {
        fun onSwipeLeft(viewHolder: VH.Value)
        fun onSwipeRight(viewHolder: VH.Value)
    }

    private inner class Decorator : RecyclerView.ItemDecoration() {
        private val space = requireContext().dpToPx(4f).toInt()
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.set(space, space, space, space)
        }
    }

    private object ItemDataCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.isEquals(newItem)
        }
    }
    private sealed class VH(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun onBind(item: Item)

        class Value(val binding: TaskPreviewBinding, val callback: TaskViewActionCallback) : VH(binding.root) {

            init {
                binding.cardView.setOnClickListener {
                    callback.onClick(this)
                }
                binding.cardView.setOnLongClickListener {
                    callback.onLongClick(this)
                }
                binding.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) callback.onChecked(this, isChecked)
                }
            }

            internal lateinit var item: Item.Value

            override fun onBind(item: Item) {
                bind(item as Item.Value)

            }

            fun bind(value: Item.Value) {
                item = value
                binding.checkBox.isChecked = value.task.isComplete
                binding.textViewDate.apply {
                    text = if (value.task.isComplete) value.task.timeOfCompletion?.let { callback.onTime(it) }
                    else callback.onTime(value.task.timeOfCreation)
                }
                binding.textViewName.text = value.task.name
                val image = item.task.image
                if (image == null) binding.imageView.setImageDrawable(null)
                else callback.onLoadImage(binding.imageView, image)
            }

        }
        class Header(val binding: TaskHeaderViewBinding) : VH(binding.root) {
            override fun onBind(item: Item) {
                bind(item as Item.Header)
            }
            fun bind(value: Item.Header) {
                binding.textView.text = value.text
            }
        }

    }

    private class Adapter(val callback: TaskViewActionCallback)
        : PagingDataAdapter<Item, VH>(ItemDataCallback) {

        companion object {
            private const val TYPE_HEADER = 1
            private const val TYPE_VALUE = 2
        }

        private var layoutInflater: LayoutInflater? = null
        fun getInflater(context: Context): LayoutInflater {
            if (layoutInflater == null)
                layoutInflater = LayoutInflater.from(context)
            return layoutInflater!!
        }

        override fun getItemViewType(position: Int): Int {
            return when(getItem(position)) {
                is Item.Header -> TYPE_HEADER
                is Item.Value -> TYPE_VALUE
                else -> 0
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return when(viewType) {
                TYPE_HEADER -> VH.Header(TaskHeaderViewBinding.inflate(getInflater(parent.context), parent, false))
                TYPE_VALUE -> {
                    VH.Value(TaskPreviewBinding.inflate(getInflater(parent.context), parent, false), callback)
                }
                else -> throw NotImplementedError()
            }
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            getItem(position)?.let {
                holder.onBind(it)
            }
        }

    }

    private class SwipeToDeleteCallback(private val callback: SwipeCallback)
        : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return true
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (viewHolder != null)
                getDefaultUIUtil().onSelected(getForegroundView(viewHolder))
        }
        override fun onChildDrawOver(
            c: Canvas, recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float, dY: Float, actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            getDefaultUIUtil().onDrawOver(c, recyclerView, getForegroundView(viewHolder), dX, dY, actionState, isCurrentlyActive)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            getDefaultUIUtil().clearView(getForegroundView(viewHolder))
        }
        override fun onChildDraw(
            c: Canvas, recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float, dY: Float, actionState: Int,
            isCurrentlyActive: Boolean,
        ) {
            getDefaultUIUtil().onDraw(c, recyclerView, getForegroundView(viewHolder), dX, dY, actionState, isCurrentlyActive)
        }
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            getViewHolder(viewHolder).item ?: return
            if (direction == ItemTouchHelper.LEFT) callback.onSwipeLeft(getViewHolder(viewHolder))
            else if (direction == ItemTouchHelper.RIGHT) callback.onSwipeRight(getViewHolder(viewHolder))
        }

        private fun getForegroundView(holder: RecyclerView.ViewHolder): View {
            return (holder as VH.Value).binding.cardView
        }
        private fun getViewHolder(viewHolder: RecyclerView.ViewHolder): VH.Value = viewHolder as VH.Value

    }

}