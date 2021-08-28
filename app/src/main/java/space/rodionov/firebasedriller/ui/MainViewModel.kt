package space.rodionov.firebasedriller.ui

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
    private val _userDataFlow = MutableStateFlow(Pair(username, email))
    val userDataFlow: StateFlow<Pair<String, String>> = _userDataFlow.asStateFlow()

    fun checkIfLoggedIn(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            auth.currentUser?.let {
                val un = it.displayName ?: "User"
                val em = it.email ?: "Email"
                _userDataFlow.value = Pair(un, em)
            }
        } else {
            _userDataFlow.value = Pair("Not logged in", "")
        }
    }
}





