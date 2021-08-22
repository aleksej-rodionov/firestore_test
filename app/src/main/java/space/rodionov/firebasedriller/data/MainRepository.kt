package space.rodionov.firebasedriller.data

import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.ui.profile.LoginViewModel
import space.rodionov.firebasedriller.ui.profile.ProfileViewModel
import java.lang.Exception
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val noteDb: NoteDatabase
) {
    val auth = FirebaseAuth.getInstance()

    val notesCollectionRef = Firebase.firestore
        .collection("notes")

//==========================ROOM METHODS=================================

    private val noteDao = noteDb.noteDao()

    fun getAllNotes() : Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    suspend fun insertNote(note: Note) = noteDao.insertNote(note)
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

}





