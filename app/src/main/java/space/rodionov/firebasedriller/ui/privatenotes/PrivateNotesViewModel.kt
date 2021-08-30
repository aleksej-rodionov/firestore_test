package space.rodionov.firebasedriller.ui.privatenotes

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.firebase.firestore.ktx.toObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.data.MainRepository
import space.rodionov.firebasedriller.data.Note
import space.rodionov.firebasedriller.ui.ADD_NOTE_RESULT_OK
import space.rodionov.firebasedriller.ui.EDIT_NOTE_RESULT_OK
import space.rodionov.firebasedriller.util.filePickerIntent
import space.rodionov.firebasedriller.util.generateFile
import space.rodionov.firebasedriller.util.goToFileIntent
import java.io.File
import javax.inject.Inject

private const val TAG = "ViewModel LOGS"

@HiltViewModel
class PrivateNotesViewModel @Inject constructor(
    private val reposiroty: MainRepository
) : ViewModel() {
    val auth = reposiroty.auth

    val notesCollectionRef = reposiroty.notesCollectionRef

    val oriList = mutableListOf<Note>()

    val csvFileName = "PrivateNotes.csv"

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
        data class GoToFileActivity(val intent: Intent) : PrivateNotesEvent()
        data class PickFileActivity(val intent: Intent) : PrivateNotesEvent()
    }

//==================================METHODS================================

    fun importDataFromCSVFile(context: Context) = viewModelScope.launch {
        val intent = filePickerIntent(context)
        Log.d(TAG, "viewModel: Intent created and sent to Channel")
        privateNotesEventChannel.send(PrivateNotesEvent.PickFileActivity(intent))
    }

    private fun exportPrivateNotesToCSVFile(csvFile: File) {
        csvWriter().open(csvFile, append = false) {
            // Header
            writeRow(listOf("[text]", "[is important]", "[is completed]", "[created at]", "[firebase author id]", "[room note id]"))
            // Body
            viewModelScope.launch {
                _notesFlow.collectLatest { notes ->
                    notes.forEach { n ->
                        writeRow(n.text, n.important, n.completed, n.created, n.authorId, n.roomId)
                    }
                }
            }
        }
    }

    fun exportDataToCSVFile(context: Context) {
        val csvFile = generateFile(context, csvFileName)
        if (csvFile != null) {
            exportPrivateNotesToCSVFile(csvFile)
            Log.d(TAG, "CSV file generated")
            val intent = goToFileIntent(context, csvFile)
            viewModelScope.launch {
                privateNotesEventChannel.send(PrivateNotesEvent.GoToFileActivity(intent))
            }
        } else {
            viewModelScope.launch {
                privateNotesEventChannel.send(PrivateNotesEvent.PrivateNotesSnackbar("CSV file not generated"))
            }
        }
    }

    fun subscribeToNotes() {
        if (auth.currentUser != null) {
            subscribeToRealtimeUpdates()
        } else {
            subscribeToFlowInRoom()
        }
    }

    fun updateNotesFlow(notes: List<Note>) {
        Log.d(TAG, "upd $notes")
        _notesFlow.value = notes
    }

    fun completedCheck(note: Note, completed: Boolean) {
        if (auth.currentUser != null) {
            completedCheckInFirestore(note, completed)
        } else {
            completedCheckInRoom(note, completed)
        }
    }

    fun deleteNote(note: Note) {
        if (auth.currentUser != null) {
            deleteNoteInFirestore(note)
        } else {
            deleteNoteInRoom(note)
        }
    }

    fun saveNote(note: Note) {
        if (auth.currentUser != null) {
            saveNoteInFirestore(note)
        } else {
            saveNoteInRoom(note)
        }
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_NOTE_RESULT_OK -> showSnackbar("Note added")
            EDIT_NOTE_RESULT_OK -> showSnackbar("Note edited")
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

//=====================================ROOM METHODS===================================

    private fun subscribeToFlowInRoom() = viewModelScope.launch {
        reposiroty.getAllNotes().collectLatest {
            updateNotesFlow(it)
        }
    }

    fun completedCheckInRoom(note: Note, completed: Boolean) = viewModelScope.launch {
        reposiroty.updateNote(note.copy(completed = completed))
    }

    fun deleteNoteInRoom(note: Note) = viewModelScope.launch {
        reposiroty.deleteNote(note)
        privateNotesEventChannel.send(
            PrivateNotesEvent.PrivateNoteInteractiveSnackbar(
                "Note deleted",
                note
            )
        )
    }

    fun saveNoteInRoom(note: Note) = viewModelScope.launch {
        reposiroty.insertNote(note)
    }

//===================================FIRESTORE METHODS===============================

    private fun subscribeToRealtimeUpdates() {
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

    fun completedCheckInFirestore(note: Note, completed: Boolean) = viewModelScope.launch {

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

    fun deleteNoteInFirestore(note: Note) = viewModelScope.launch {

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

    fun saveNoteInFirestore(note: Note) = viewModelScope.launch {
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
}





