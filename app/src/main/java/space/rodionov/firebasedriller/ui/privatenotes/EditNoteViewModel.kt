package space.rodionov.firebasedriller.ui.privatenotes

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.data.FirestoreRepository
import space.rodionov.firebasedriller.data.Note
import space.rodionov.firebasedriller.ui.ADD_NOTE_RESULT_OK
import space.rodionov.firebasedriller.ui.EDIT_NOTE_RESULT_OK
import javax.inject.Inject

private const val TAG = "EditNoteViewModel LOGS"

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    //val auth = FirebaseAuth.getInstance()
    val auth = repository.auth

    //val notesCollectionRef = Firebase.firestore.collection("notes")
    val notesCollectionRef = repository.notesCollectionRef

//=================================SAVED STATE HANDLE=============================

    val title = state.get<String>("title")

    val note = state.get<Note>("note")

    var noteText = state.get<String>("noteText") ?: note?.text ?: ""
        set(value) {
            field = value
            state.set("noteText", value)
        }
    var notePriority = state.get<Boolean>("notePriority") ?: note?.important ?: false
        set(value) {
            field = value
            state.set("notePriority", value)
        }
    var noteComplited = state.get<Boolean>("noteCompleted") ?: note?.completed ?: false
        set(value) {
            field = value
            state.set("noteComplited", value)
        }
    val noteAuthorId =
        state.get<String>("noteAuthorId") ?: note?.authorId ?: auth.currentUser?.uid ?: ""

//=======================================EVENT CHANNEL=============================

    private val editNoteEventChannel = Channel<EditNoteEvent>()
    val editNoteEvent = editNoteEventChannel.receiveAsFlow()

    sealed class EditNoteEvent {
        data class EditNoteSnackbar(val msg: String) : EditNoteEvent()
        data class NavBackWithResult(val result: Int) : EditNoteEvent()
    }

//===================================METHODS======================================

    fun onSaveClick() {
        if (noteText.isBlank()) {
            showMsg("Enter note text")
            return
        }

        if (auth.currentUser == null) {
            showMsg("You need to log in to create and edit notes")
            return
        }

        if (note != null) {
            val noteMap = getNewNoteMap()
            updateNote(note, noteMap)
        } else {
            if (auth.currentUser == null) {
                showMsg("You need to log in to create notes")
                return
            }

            auth.currentUser?.let {
                val newNote = Note(noteText, notePriority, false, authorId = it.uid)
                createNote(newNote)
            }
        }
    }

    private fun updateNote(oldNote: Note, newNoteMap: Map<String, Any>) = viewModelScope.launch {
        val noteQuery = notesCollectionRef
            .whereEqualTo("text", oldNote.text)
            .whereEqualTo("important", oldNote.important)
            .whereEqualTo("completed", oldNote.completed)
            .whereEqualTo("created", oldNote.created)
            .whereEqualTo("authorId", oldNote.authorId)
            .get()
            .await()
        if (noteQuery.documents.isNotEmpty()) {
            for (document in noteQuery) {
                try {
                    notesCollectionRef.document(document.id).set(
                        newNoteMap,
                        SetOptions.merge()
                    ).await()
                    editNoteEventChannel.send(EditNoteEvent.NavBackWithResult(EDIT_NOTE_RESULT_OK))
                } catch (e: Exception) {
                    editNoteEventChannel.send(
                        EditNoteEvent.EditNoteSnackbar(
                            e.message ?: "Unknown firestore exception"
                        )
                    )
                }
            }
        } else {
            editNoteEventChannel.send(EditNoteEvent.EditNoteSnackbar("No note matched the query"))
        }
    }

    fun showUserId() = viewModelScope.launch {
        auth.currentUser?.let {
            showMsg(it.uid)
        }
    }

    private fun getNewNoteMap(): Map<String, Any> {
        val text = noteText
        val priority = notePriority
        val completed = noteComplited
        val created = note?.created ?: System.currentTimeMillis()
        val authorId = noteAuthorId
        val map = mutableMapOf<String, Any>()
        if (text.isNotEmpty()) map["text"] = text
        map["important"] = priority
        map["completed"] = completed
        map["created"] = created
        map["authorId"] = authorId
        Log.d(TAG, "getNewNoteMap: $map")
        return map
    }

    private fun showMsg(msg: String) = viewModelScope.launch {
        editNoteEventChannel.send(EditNoteEvent.EditNoteSnackbar(msg))
    }

    private fun createNote(note: Note) = viewModelScope.launch {
        try {
            notesCollectionRef.add(note).await()
            editNoteEventChannel.send(EditNoteEvent.NavBackWithResult(ADD_NOTE_RESULT_OK))
        } catch (e: Exception) {
            editNoteEventChannel.send(
                EditNoteEvent.EditNoteSnackbar(
                    e.message ?: "Unknown exception"
                )
            )
        }
    }
}





