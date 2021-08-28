package space.rodionov.firebasedriller.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import space.rodionov.firebasedriller.data.MainRepository
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val repository: MainRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    val localNotes = repository.getAllNotes()

//===========================EVENT CHANNEL=============================

    private var welcomeEventChannel = Channel<WelcomeEvent>()
    val welcomeEvent = welcomeEventChannel.receiveAsFlow()

    sealed class WelcomeEvent {
        object NavWelcomeToNotes : WelcomeEvent()
        object NavWelcomeToLocalNotes : WelcomeEvent()
    }

//================================METHODS====================================

    fun onLookLocalNotes() = viewModelScope.launch {
        welcomeEventChannel.send(WelcomeEvent.NavWelcomeToLocalNotes)
    }

    fun onSkip() = viewModelScope.launch {
        welcomeEventChannel.send(WelcomeEvent.NavWelcomeToNotes)
    }
}





