package space.rodionov.firebasedriller.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val state: SavedStateHandle
) : ViewModel() {

    private var welcomeEventChannel = Channel<WelcomeEvent>()
    val welcomeEvent = welcomeEventChannel.receiveAsFlow()

    sealed class WelcomeEvent {
        object NavWelcomeToNotes : WelcomeEvent()
    }

    fun ok() = viewModelScope.launch {
        welcomeEventChannel.send(WelcomeEvent.NavWelcomeToNotes)
    }


}





