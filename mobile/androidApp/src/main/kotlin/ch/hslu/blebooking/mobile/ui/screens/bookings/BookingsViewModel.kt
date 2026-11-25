package ch.hslu.blebooking.mobile.ui.screens.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.api.MobileCoreApi
import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BookingsState {
    data object Loading : BookingsState
    data class Ready(val bookings: List<Booking>) : BookingsState
    data class Failed(val sessionExpired: Boolean = false) : BookingsState
}

@HiltViewModel
class BookingsViewModel @Inject constructor(private val api: MobileCoreApi) : ViewModel() {
    private val _state = MutableStateFlow<BookingsState>(BookingsState.Loading)
    val state = _state.asStateFlow()
    private var loading = false

    init { loadBookings() }

    fun loadBookings() {
        if (loading) return
        loading = true
        _state.value = BookingsState.Loading
        viewModelScope.launch {
            try {
                _state.value = BookingsState.Ready(api.getActiveBookings().sortedBy { it.startTime })
            } catch (e: CancellationException) {
                throw e
            } catch (e: MobileCoreException) {
                _state.value = BookingsState.Failed(e.code == MobileCoreErrorCode.UNAUTHORIZED)
            } catch (_: Exception) {
                _state.value = BookingsState.Failed()
            } finally {
                loading = false
            }
        }
    }
}
