package space.rodionov.firebasedriller.ui.privatenotes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.data.FirestoreRepository
import space.rodionov.firebasedriller.data.Note
import space.rodionov.firebasedriller.ui.ADD_NOTE_RESULT_OK
import space.rodionov.firebasedriller.ui.EDIT_NOTE_RESULT_OK
import javax.inject.Inject

private const val TAG = "ViewModel LOGS"

@HiltViewModel
class PrivateNotesViewModel @Inject constructor(
    private val reposiroty: FirestoreRepository
) : ViewModel() {
    //val auth = FirebaseAuth.getInstance()
    val auth = reposiroty.auth

    //val notesCollectionRef = Firebase.firestore.collection("notes")
    val notesCollectionRef = reposiroty.notesCollectionRef


    val oriList = mutableListOf<Note>()

//===============================FLOWS======================================

    private val _notesFlow = MutableStateFlow<List<Note>>(oriList)
    val notesFlow = _notesFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

//=============================EVENT CHANNEL=============================

    private val privateNotesEventChannel = Channel<PrivateNotesEvent>()
    val privateNotesEvent = privateNotesEventChannel.receiveAsFlow()

    sealed class PrivateNotesEvent {
        data class PrivateNotesSnackbar(val msg: String) : PrivateNotesEvent()
        object NavAddNote : PrivateNotesEvent()
        data class NavEditNote(val note: Note) : PrivateNotesEvent()
        data class PrivateNoteInteractiveSnackbar(val msg: String, val note: Note) :
            PrivateNotesEvent()
    }

//==================================METHODS================================

    fun subscribeToRealtimeUpdates() {
        if (auth.currentUser == null) {
            showSnackbar("You need to log in to see your notes")
            _notesFlow.value = oriList
            return
        }

        val uid = auth.currentUser?.uid

        val notesByUid = notesCollectionRef.whereEqualTo("authorId", uid)

        notesByUid.addSnapshotListener { querySnapshot, firestoreException ->
            firestoreException?.let {
                showSnackbar("(SNAPSHOT)" + (it.message ?: "Unknown firestore exception"))
                return@addSnapshotListener
            }
            querySnapshot?.let { shapshot ->
                val notes = mutableListOf<Note>()
                // 5 часов разбирался с проблемой что список не обновлялся в UI моментально, и решилось когда переместил сюда эту дкаларирование. Почему?
                // потоучто лист задекларирован 1 раз при отКриейт. А тут меняется только состав его или параметры отдельных пунктов. А флоу не наблюдает смену пунктов (насчет состава хз). Он видит только что лист один и тотже с одним и темже названием. И Коллект не срабатывает.
                // А тут ты делаешь каждый раз новый лист именно в самом QuerySnapshot {}. И этот новый лист запускаешь, и флоу обновляется. И коллект срабатывает.
                for (document in shapshot) {
                    val note = document.toObject<Note>()
                    notes.add(note)
                }
                updateNotesFlow(notes)
            }
        }
    }

    fun updateNotesFlow(notes: List<Note>) {
        Log.d(TAG, "upd $notes")
        _notesFlow.value = notes
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_NOTE_RESULT_OK -> showSnackbar("Note added")
            EDIT_NOTE_RESULT_OK -> showSnackbar("Note edited")
        }
    }

    fun completedCheck(note: Note, completed: Boolean) = viewModelScope.launch {

        auth.currentUser?.let {
            val noteQuery = notesCollectionRef
                .whereEqualTo("text", note.text)
                .whereEqualTo("important", note.important)
                .whereEqualTo("completed", note.completed)
                .whereEqualTo("created", note.created)
                .whereEqualTo("authorId", note.authorId)
                .get()
                .await()
            if (noteQuery.documents.isNotEmpty()) {
                for (document in noteQuery) {
                    try {
                        notesCollectionRef.document(document.id).update("completed", completed)
                    } catch (e: Exception) {
                        privateNotesEventChannel.send(
                            PrivateNotesEvent.PrivateNotesSnackbar(
                                e.message ?: "Unknown firestore exception"
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteNote(note: Note) = viewModelScope.launch {

        auth.currentUser?.let {
            val noteQuery = notesCollectionRef
                .whereEqualTo("text", note.text)
                .whereEqualTo("important", note.important)
                .whereEqualTo("completed", note.completed)
                .whereEqualTo("created", note.created)
                .whereEqualTo("authorId", note.authorId)
                .get()
                .await()
            if (noteQuery.documents.isNotEmpty()) {
                for (document in noteQuery) {
                    try {
                        notesCollectionRef.document(document.id).delete().await()
                        privateNotesEventChannel.send(
                            PrivateNotesEvent.PrivateNoteInteractiveSnackbar(
                                "Note deleted",
                                note
                            )
                        )
                    } catch (e: Exception) {
                        privateNotesEventChannel.send(
                            PrivateNotesEvent.PrivateNotesSnackbar(
                                e.message ?: "Unknown firestore exception"
                            )
                        )
                    }
                }
            } else {
                privateNotesEventChannel.send(PrivateNotesEvent.PrivateNotesSnackbar("No note matched the query"))
            }
        }
    }

    fun saveNote(note: Note) = viewModelScope.launch {
        try {
            notesCollectionRef.add(note).await()
        } catch (e: Exception) {
            privateNotesEventChannel.send(
                PrivateNotesEvent.PrivateNotesSnackbar(
                    e.message ?: "Unknown exception"
                )
            )
        }
    }

    fun editNote(note: Note) = viewModelScope.launch {
        privateNotesEventChannel.send(PrivateNotesEvent.NavEditNote(note))
    }

    fun showSnackbar(msg: String) = viewModelScope.launch {
        privateNotesEventChannel.send(PrivateNotesEvent.PrivateNotesSnackbar(msg))
    }

    fun newNote() = viewModelScope.launch {
        privateNotesEventChannel.send(PrivateNotesEvent.NavAddNote)
    }
}





