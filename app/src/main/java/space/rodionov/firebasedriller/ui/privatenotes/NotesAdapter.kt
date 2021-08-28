package space.rodionov.firebasedriller.ui.privatenotes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import space.rodionov.firebasedriller.data.Note
import space.rodionov.firebasedriller.databinding.ItemNoteBinding
import space.rodionov.firebasedriller.util.NoteComparator

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onCompletedCheck: (Note, Boolean) -> Unit
) : ListAdapter<Note, NotesAdapter.NotesViewHolder>(NoteComparator()) {

    inner class NotesViewHolder(
        private val binding: ItemNoteBinding,
        private val onItemClick: (Int) -> Unit,
        private val onCheckBoxChecked: (Int, Boolean) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            binding.apply {
                tvNote.text = note.text
                cbCompleted.isChecked = note.completed
                ivPriority.isVisible = note.important
            }
        }

        init {
            binding.apply {
                root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(position)
                    }
                }
                cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onCheckBoxChecked(position, isChecked)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesViewHolder(
            binding,
            onItemClick = { pos ->
                val note = getItem(pos)
                if (note != null) {
                    onNoteClick(note)
                }
            },
            onCheckBoxChecked = { pos, isChecked ->
                val note = getItem(pos)
                if (note != null) {
                    onCompletedCheck(note, isChecked)
                }
            }
        )
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        val curItem = getItem(position)
        holder.bind(curItem)
    }
}