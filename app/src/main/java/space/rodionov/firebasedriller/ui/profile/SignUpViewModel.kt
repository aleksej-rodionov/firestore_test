package space.rodionov.firebasedriller.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.data.MainRepository
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: MainRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    var em = state.get<String>("email") ?: ""
        set(value) {
            field = value
            state.set("email", value)
        }
    var pw = state.get<String>("password") ?: ""
        set(value) {
            field = value
            state.set("password", value)
        }
    var un = state.get<String>("username") ?: ""
        set(value) {
            field = value
            state.set("username", value)
        }

    val actionMade = "registered"

val auth = repository.auth

//==========================EVENT CHANNEL===========================

    private val signUpEventChannel = Channel<SignUpEvent>()
    val signUpEvent = signUpEventChannel.receiveAsFlow()

    sealed class SignUpEvent {
        data class NavigateSignedUp(val em: String, val pw: String, val ma: String) : SignUpEvent()
        data class SignUpSnackbar(val msg: String) : SignUpEvent()

    }

//=============================METHODS===================================

    fun registerUserEmailAndPassword() {
        val username = un
        val email = em
        val password = pw
        if (email.isNotEmpty() && password.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    auth.createUserWithEmailAndPassword(email, password).await() // where magic happens
                    //here you can put loading circle show up
                    updateUsername()
                    checkLoggedInState()
                } catch (e: Exception) {
                    signUpEventChannel.send(
                        SignUpEvent.SignUpSnackbar(
                            e.message ?: "Unknown exception"
                        )
                    )
                }
            }
        }
    }

    private fun checkLoggedInState() = viewModelScope.launch {
        if (auth.currentUser == null) {
            signUpEventChannel.send(SignUpEvent.SignUpSnackbar("Could not log in"))
        } else {
            signUpEventChannel.send(SignUpEvent.NavigateSignedUp(em, pw, actionMade))
        }
    }

    fun updateUsername() {
        auth.currentUser?.let { user ->
            val username = un
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()

            viewModelScope.launch {
                try {
                    user.updateProfile(profileUpdates).await()
                    signUpEventChannel.send(SignUpEvent.SignUpSnackbar("Successfully updated profile"))
                } catch (e: java.lang.Exception) {
                    signUpEventChannel.send(
                        SignUpEvent.SignUpSnackbar(
                            e.message ?: "Unknown exception"
                        )
                    )
                }
            }
        }
    }
}





