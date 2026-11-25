package ch.hslu.blebooking.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.api.MobileCoreApi
import ch.hslu.blebooking.mobile.storage.AndroidSecureStorage
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
}

@HiltViewModel
class AppViewModel @Inject constructor(
    private val mobileCoreApi: MobileCoreApi,
    private val secureStorage: AndroidSecureStorage,
) : ViewModel() {

    private val _initialState = MutableStateFlow<AuthInitialState>(AuthInitialState.Loading)
    val initialState: StateFlow<AuthInitialState> = _initialState.asStateFlow()

    init {
        checkExistingSession()
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            val refreshToken = try {
                secureStorage.get("refreshToken")
            } catch (_: Exception) {
                null
            }

            if (refreshToken.isNullOrBlank()) {
                _initialState.value = AuthInitialState.Unauthenticated
                return@launch
            }

            try {
                // Trying to fetch the profile triggers Ktor Auth's automatic bearer refresh
                mobileCoreApi.getUserProfile()
                _initialState.value = AuthInitialState.Authenticated
            } catch (_: Exception) {
                // Token invalid or refresh failed: clear storage and send to login
                try {
                    secureStorage.remove("refreshToken")
                } catch (_: Exception) {
                    // Ignore removal failures
                }
                _initialState.value = AuthInitialState.Unauthenticated
            }
        }
    }
}
