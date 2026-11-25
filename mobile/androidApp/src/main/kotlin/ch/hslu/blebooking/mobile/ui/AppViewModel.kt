package ch.hslu.blebooking.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.api.MobileCoreApi
import ch.hslu.blebooking.mobile.model.SessionRestoreResult
import kotlinx.coroutines.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthInitialState {
    data object Loading : AuthInitialState
    data object Authenticated : AuthInitialState
    data object Unauthenticated : AuthInitialState
    data object Failed : AuthInitialState
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val mobileCoreApi: MobileCoreApi,
) : ViewModel() {

    private val _initialState = MutableStateFlow<AuthInitialState>(AuthInitialState.Loading)
    val initialState: StateFlow<AuthInitialState> = _initialState.asStateFlow()

    private var restoreJob: Job? = null

    init {
        checkExistingSession()
    }

    fun checkExistingSession() {
        if (restoreJob?.isActive == true) return
        _initialState.value = AuthInitialState.Loading
        restoreJob = viewModelScope.launch {
            _initialState.value = when (mobileCoreApi.restoreSession()) {
                is SessionRestoreResult.Authenticated -> AuthInitialState.Authenticated
                SessionRestoreResult.Unauthenticated -> AuthInitialState.Unauthenticated
                is SessionRestoreResult.Failed -> AuthInitialState.Failed
            }
        }
    }
}
