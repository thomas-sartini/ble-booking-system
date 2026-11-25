package ch.hslu.blebooking.mobile.ui.screens.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.api.MobileCoreApi
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import ch.hslu.blebooking.mobile.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mobileCoreApi: MobileCoreApi,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _userProfile.value = mobileCoreApi.getUserProfile()
            } catch (_: Exception) {
                // Silently ignore or fallback
            }
        }
    }

    fun logout(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            try {
                mobileCoreApi.logout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: MobileCoreException) {
                onError(e.message ?: e.code.message)
                return@launch
            } catch (_: Exception) {
                onError(context.getString(R.string.logout_error))
                return@launch
            } finally {
                isLoading = false
            }
            onSuccess()
        }
    }
}
