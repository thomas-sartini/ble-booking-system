package ch.hslu.blebooking.mobile.ui.screens.login

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.api.MobileCoreApi
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import ch.hslu.blebooking.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val mobileCoreApi: MobileCoreApi,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    fun signIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (isLoading) return
        if (email.isBlank() || password.isEmpty()) {
            onError(context.getString(R.string.login_error_required_fields))
            return
        }
        isLoading = true
        viewModelScope.launch {
            try {
                mobileCoreApi.login(email.trim(), password)
            } catch (e: CancellationException) {
                throw e
            } catch (e: MobileCoreException) {
                Log.e("LoginViewModel", "Login failed: ${e.code}, ${e.message}", e)
                onError(e.message ?: e.code.message)
                return@launch
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login failed:", e)
                onError(context.getString(R.string.login_error_failed))
                return@launch
            } finally {
                isLoading = false
            }
            onSuccess()
        }
    }
}
