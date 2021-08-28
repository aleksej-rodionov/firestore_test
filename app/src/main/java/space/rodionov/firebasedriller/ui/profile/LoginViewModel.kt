package space.rodionov.firebasedriller.ui.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import space.rodionov.firebasedriller.data.MainRepository
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: MainRepository,
    private val state: SavedStateHandle
) : ViewModel() {
val auth = repository.auth

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

    val emailLoggedInMsg = "logged in"
    val googleLoggedInMsg = "logged in with Google"

//==========================EVENT CHANNEL===========================

    private val loginEventChannel = Channel<LoginEvent>()
    val loginEvent = loginEventChannel.receiveAsFlow()

    sealed class LoginEvent {
        data class NavigateLoggedIn(val em: String?, val pw: String?, val ma: String) : LoginEvent()
        object NavigateNotRegistered : LoginEvent()
        data class LoginSnackbar(val msg: String) : LoginEvent()
        data class LoginActivity(val intent: Intent) : LoginEvent()
    }

//=============================METHODS===================================

    fun onGoogleSignIn(context: Context, webclient_id: String) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webclient_id)
            .requestEmail()
            .build()
        val signInClient = GoogleSignIn.getClient(context, options)
        val intent = signInClient.signInIntent
        viewModelScope.launch {
            loginEventChannel.send(LoginEvent.LoginActivity(intent))
        }
    }

    fun googleAuthForFirebase(account: GoogleSignInAccount) { // запускаем гугл-аккаунт в качестве аргумента
        val credentials =  GoogleAuthProvider.getCredential(account.idToken, null) // получаем реквизиты для входа в гуглАккаунт, запущенный как аргумент в эту фенкцию
        viewModelScope.launch {
            try {
                auth.signInWithCredential(credentials).await()
                loginEventChannel.send(LoginEvent.NavigateLoggedIn(null, null, googleLoggedInMsg))
            } catch (e: Exception) {
                loginEventChannel.send(LoginEvent.LoginSnackbar(e.message ?: "Unknown exception"))
            }
        }
    }

    fun onResultCanceled() = viewModelScope.launch {
        loginEventChannel.send(LoginEvent.LoginSnackbar("ResultCode is RESULT_CANCELED"))
    }

    fun signUp() = viewModelScope.launch {
        loginEventChannel.send(LoginEvent.NavigateNotRegistered)
    }

    fun loginUser() {
        val email = em
        val password = pw
        if (email.isNotEmpty() && password.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    auth.signInWithEmailAndPassword(email, password).await() // where magic happens
                    //here you can put loading circle show up
                    checkLoggedInState()
                } catch (e: Exception) {
                    loginEventChannel.send(
                        LoginEvent.LoginSnackbar(
                            e.message ?: "Unknown exception"
                        )
                    )
                }
            }
        }
    }

    private fun checkLoggedInState() = viewModelScope.launch {
        if (auth.currentUser == null) {
            loginEventChannel.send(LoginEvent.LoginSnackbar("Could not log in"))
        } else {
            loginEventChannel.send(LoginEvent.NavigateLoggedIn(em, pw, emailLoggedInMsg))
        }
    }
}





