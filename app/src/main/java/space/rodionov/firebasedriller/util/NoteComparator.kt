package space.rodionov.firebasedriller.util

import androidx.recyclerview.widget.DiffUtil
import space.rodionov.firebasedriller.data.Note

class NoteComparator: DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: Note, newItem: Note) =
        oldItem.text == newItem.text
                && oldItem.created == newItem.created
                && oldItem.important == newItem.important
                && oldItem.completed == newItem.completed

}