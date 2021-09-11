package space.rodionov.firebasedriller.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.rodionov.firebasedriller.data.MainRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {
    val auth = repository.auth

    var username = auth.currentUser?.displayName ?: "Not logged in"
    val email = auth.currentUser?.email ?: ""
    val photoUrl = auth.currentUser?.photoUrl
    private val _userDataFlow = MutableStateFlow(Triple(username, email, photoUrl))
    val userDataFlow: StateFlow<Triple<String, String, Uri?>> = _userDataFlow.asStateFlow()

    fun checkIfLoggedIn(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            auth.currentUser?.let {
                val un = it.displayName ?: "User"
                val em = it.email ?: "Email"
                val pu = it.photoUrl
                _userDataFlow.value = Triple(un, em, pu)
            }
        } else {
            _userDataFlow.value = Triple("Not logged in", "", null)
        }
    }
}





