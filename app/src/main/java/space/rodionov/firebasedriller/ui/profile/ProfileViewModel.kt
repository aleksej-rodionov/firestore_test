package space.rodionov.firebasedriller.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.data.FirestoreRepository
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val state: SavedStateHandle
) : ViewModel() {
//    val auth = FirebaseAuth.getInstance()
    val auth = repository.auth

    var un = state.get<String>("username") ?: auth.currentUser?.displayName ?: ""
        set(value) {
            field = value
            state.set("username", value)
        }


    private var _loggedInState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val loggedInState = _loggedInState.stateIn(viewModelScope, SharingStarted.Lazily, null)

//===================================EVENT CHANNEL===============================

    private val profileEventChannel = Channel<ProfileEvent>()
    val profileEvent = profileEventChannel.receiveAsFlow()

    sealed class ProfileEvent {
        object NavigateToLogin: ProfileEvent()
        data class ProfileSnackbar(val msg: String) : ProfileEvent()
    }

//===========================REPO CALLS======================================

//    fun onLogout() {
//        FirestoreRepository.signOut()
//        _loggedInState.value = null
//    }
//
//    fun updateUsername() {
//        val username = un
//        val errorMsg = FirestoreRepository.updateUsername(viewModelScope, username)
//        viewModelScope.launch {
//            if (errorMsg != null) {
//                profileEventChannel.send(ProfileEvent.ProfileSnackbar(errorMsg))
//            }else {
//                profileEventChannel.send(ProfileEvent.ProfileSnackbar("Successfully updated profile"))
//            }
//        }
//    }

//===========================METHODS==========================================

    fun onLogin() = viewModelScope.launch {
        profileEventChannel.send(ProfileEvent.NavigateToLogin)
    }

    fun onLogout() {
        auth.signOut()
        _loggedInState.value = null
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
                    profileEventChannel.send(ProfileEvent.ProfileSnackbar("Successfully updated profile"))
                } catch (e: Exception) {
                    profileEventChannel.send(
                        ProfileEvent.ProfileSnackbar(
                            e.message ?: "Unknown exception"
                        )
                    )
                }
            }
        }
    }
}





