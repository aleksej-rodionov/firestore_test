package space.rodionov.firebasedriller.ui.profile

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.data.MainRepository
import space.rodionov.firebasedriller.data.Note
import space.rodionov.firebasedriller.ui.ADD_NOTE_RESULT_OK
import space.rodionov.firebasedriller.ui.privatenotes.EditNoteViewModel
import javax.inject.Inject

private const val TAG = "ViewModel LOGS"

@HiltViewModel
class UploadLocalNotesViewModel @Inject constructor(
    private val repository: MainRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    val auth = repository.auth
    val userId = auth.currentUser?.uid ?: ""
    val notesCollectionRef = repository.notesCollectionRef

    var bufferJson = state.getLiveData("bufferList", "[]")
    var areAllChecked = state.getLiveData<Boolean>("areAllChecked", false)

    private val gson = Gson()

    var notes = mutableListOf<Note>()

    private val bufferIds: MutableList<Int> =
        gson.fromJson(bufferJson.value, object : TypeToken<MutableList<Int>>() {}.type)

    val allLocalNotes = repository.getAllNotes()

    private val _bufferFlow = MutableStateFlow(bufferIds)
    val bufferFlow = _bufferFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

//===================================EVENT CHANNEL================================

    private val uploadLocalNotesSnackbarChannel = Channel<UploadLocalNotesEvent>()
    val uploadLocalNotesEvent = uploadLocalNotesSnackbarChannel.receiveAsFlow()

    sealed class UploadLocalNotesEvent {
        data class UploadLocalNotesSnackbar(val msg: String) : UploadLocalNotesEvent()
        object NavigateUploadLocalToPrivateNotes : UploadLocalNotesEvent()
    }

//==================================METHODS=======================================

    fun upload() {
        notes.filter {
            bufferIds.contains(it.roomId)
        }.forEach { noteToUpload ->
            val newNote = noteToUpload.copy(authorId = userId)
            createNote(newNote)
        }
        bufferJson.value = gson.toJson(bufferIds)
    }

    private fun createNote(note: Note) = viewModelScope.launch {
        try {
            notesCollectionRef.add(note).await()
            repository.deleteNote(note)
            bufferIds.remove(note.roomId)
        } catch (e: Exception) {
            uploadLocalNotesSnackbarChannel.send(
                UploadLocalNotesEvent.UploadLocalNotesSnackbar(
                    e.message ?: "Unknown exception"
                )
            )
        }
    }

    fun onCheckAll(isChecked: Boolean) {
        if (isChecked) {
            for (note in notes) {
                if (note.roomId !in bufferIds) bufferIds.add(note.roomId)
            }
            areAllChecked.value = true
        } else {
            if (notes.size == bufferIds.size) {
                for (note in notes) {
                    if (note.roomId in bufferIds) bufferIds.remove(note.roomId)
                }
            }
            areAllChecked.value = false
        }
        Log.d(TAG, "bufferList = $bufferIds")
        bufferJson.value = gson.toJson(bufferIds)
    }

    fun onNoteCheck(note: Note, isChecked: Boolean) {
        if (isChecked) {
            if (note.roomId !in bufferIds) bufferIds.add(note.roomId)
            if (bufferIds.size == notes.size) areAllChecked.value = true
        } else {
            if (note.roomId in bufferIds) bufferIds.remove(note.roomId)
            areAllChecked.value = false
        }
        Log.d(TAG, "bufferList = $bufferIds")
        bufferJson.value = gson.toJson(bufferIds)
    }

    fun goToPrivateNotes() = viewModelScope.launch {
        uploadLocalNotesSnackbarChannel.send(UploadLocalNotesEvent.NavigateUploadLocalToPrivateNotes)
    }
}





