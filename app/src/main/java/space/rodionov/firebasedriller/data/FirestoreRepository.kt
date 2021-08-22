package space.rodionov.firebasedriller.data

import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.ui.profile.LoginViewModel
import space.rodionov.firebasedriller.ui.profile.ProfileViewModel
import java.lang.Exception
import javax.inject.Inject

class FirestoreRepository @Inject constructor(
    private val noteDb: NoteDatabase
) {
    val auth = FirebaseAuth.getInstance()

    val notesCollectionRef = Firebase.firestore
        .collection("notes")



    private val noteDao = noteDb.noteDao()







//==============================PRIVATE NOTES============================================



//===============================USER PROFILE=========================================

//    fun loginUser(scope: CoroutineScope, email: String, password: String) : String? {
//        var errorMsg: String? = null
//        if (email.isNotEmpty() && password.isNotEmpty()) {
//            scope.launch {
//                try {
//                    auth.signInWithEmailAndPassword(email, password).await() // where magic happens
//                    //here you can put loading circle show up
//                    errorMsg = checkLoggedInState(scope)
//                } catch (e: Exception) {
//                    errorMsg = e.message ?: "Unknown exception"
//                }
//            }
//        }
//        return errorMsg
//    }
//
//    fun registerUserEmailAndPassword(
//        scope: CoroutineScope,
//        username: String,
//        email: String,
//        password: String
//    ): String? {
//        var errorMsg: String? = null
//        if (email.isNotEmpty() && password.isNotEmpty()) {
//            scope.launch {
//                try {
//                    auth.createUserWithEmailAndPassword(email, password).await()
//                    errorMsg = checkLoggedInState(scope)
//                    if (errorMsg == null) {
//                        errorMsg = updateUsername(scope, username)
//                    }
//                } catch (e: Exception) {
//                    errorMsg = e.message ?: "Unknown exception"
//                }
//            }
//        }
//        return errorMsg
//    }
//
//    private fun checkLoggedInState(scope: CoroutineScope): String? {
//        var errorMsg: String? = null
//        scope.launch {
//            if (auth.currentUser == null) {
//                errorMsg = "Could not log in"
//            }
//        }
//        return errorMsg
//    }
//
//    fun updateUsername(scope: CoroutineScope, username: String): String? {
//        var errorMsg: String? = null
//        auth.currentUser?.let { user ->
//            val profileUpdates = UserProfileChangeRequest.Builder()
//                .setDisplayName(username)
//                .build()
//
//            scope.launch {
//                try {
//                    user.updateProfile(profileUpdates).await()
//                } catch (e: Exception) {
//                    errorMsg = e.message ?: "Unknown exception"
//                }
//            }
//        }
//        return errorMsg
//    }
//
//    fun googleAuthForFirebase(scope: CoroutineScope, account: GoogleSignInAccount) : String? { // запускаем гугл-аккаунт в качестве аргумента
//        val credentials =  GoogleAuthProvider.getCredential(account.idToken, null) // получаем реквизиты для входа в гуглАккаунт, запущенный как аргумент в эту фенкцию
//        var errorMsg: String? = null
//        scope.launch {
//            try {
//                auth.signInWithCredential(credentials).await()
//            } catch (e: Exception) {
//                errorMsg = e.message ?: "Unknown exception"
//            }
//        }
//        return errorMsg
//    }
//
//    fun signOut() {
//        auth.signOut()
//    }
}





