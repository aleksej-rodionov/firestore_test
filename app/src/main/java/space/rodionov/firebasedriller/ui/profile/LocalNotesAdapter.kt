package space.rodionov.firebasedriller.ui.profile

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import space.rodionov.firebasedriller.data.Note
import space.rodionov.firebasedriller.databinding.ItemLocalNoteBinding
import space.rodionov.firebasedriller.util.NoteComparator

private const val TAG = "Adapter LOGS"

class LocalNotesAdapter(
    private val scope: CoroutineScope,
    private val bufferIds: StateFlow<List<Int>?>,
    private val onNoteCheck: (Note, Boolean) -> Unit
) : ListAdapter<Note, LocalNotesAdapter.LocalNoteViewHolder>(NoteComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalNoteViewHolder {
        val binding =
            ItemLocalNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocalNoteViewHolder(binding,
            onItemChecked = { pos, isChecked ->
                val note = getItem(pos)
                if (note != null) {
                    onNoteCheck(note, isChecked)
                }
            })
    }

    override fun onBindViewHolder(holder: LocalNoteViewHolder, position: Int) {
        val curNote = getItem(position)
        holder.bind(curNote)
    }

    inner class LocalNoteViewHolder(
        private val binding: ItemLocalNoteBinding,
        private val onItemChecked: (Int, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            binding.apply {
                cbNote.text = note.text
                scope.launch {
                    bufferIds.collect {
                        val bufferedIds = it ?: return@collect
                        Log.d(TAG, "bind: buffered Ids: $bufferedIds")
                        cbNote.isChecked = bufferedIds.contains(note.roomId)
                    }
                }
                ivDone.visibility = View.INVISIBLE // change this condition
            }
        }

        init {
            binding.apply {
                cbNote.setOnCheckedChangeListener { _, isChecked ->
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemChecked(position, isChecked)
                    }
                }
            }
        }
    }

    fun setAllVisible() {
        for (i in 0 until currentList.size) {

        }
    }
}





