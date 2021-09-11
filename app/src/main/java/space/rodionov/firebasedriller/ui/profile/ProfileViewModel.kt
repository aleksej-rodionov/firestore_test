package space.rodionov.firebasedriller.ui.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import space.rodionov.firebasedriller.data.MainRepository
import space.rodionov.firebasedriller.util.imagePickerIntent
import javax.inject.Inject

private const val TAG = "ProfileViewModel LOGS"

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: MainRepository,
    private val state: SavedStateHandle
) : ViewModel() {
    val auth = repository.auth

    var un = state.get<String>("username") ?: auth.currentUser?.displayName ?: ""
        set(value) {
            field = value
            state.set("username", value)
        }

    var pu = state.get<Uri>("photoUrl") ?: auth.currentUser?.photoUrl
        set(value) {
            field = value
            state.set("photoUrl", value)
        }

    private var _loggedInState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val loggedInState = _loggedInState.stateIn(viewModelScope, SharingStarted.Lazily, null)

//===================================EVENT CHANNEL===============================

    private val profileEventChannel = Channel<ProfileEvent>()
    val profileEvent = profileEventChannel.receiveAsFlow()

    sealed class ProfileEvent {
        object NavigateToLogin : ProfileEvent()
        data class ProfileSnackbar(val msg: String) : ProfileEvent()
        data class CameraPermissionActivity(val perms: Array<String>) : ProfileEvent()
        data class PickImageActivity(val intent: Intent) : ProfileEvent()
    }

//===========================METHODS==========================================

    fun updatePhoto(photoUri: Uri?) {
        auth.currentUser?.let { user ->
            val photoUrl = pu
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(photoUrl)
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

    fun pickImage(context: Context) = viewModelScope.launch {
        val intent = imagePickerIntent(context)
        profileEventChannel.send(ProfileEvent.PickImageActivity(intent))
    }

    fun onLogin() = viewModelScope.launch {
        profileEventChannel.send(ProfileEvent.NavigateToLogin)
    }

    fun onLogout() {
        auth.signOut()
        _loggedInState.value = null
    }

    fun profileSnackbar(msg: String) = viewModelScope.launch {
        profileEventChannel.send(ProfileEvent.ProfileSnackbar(msg))
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





