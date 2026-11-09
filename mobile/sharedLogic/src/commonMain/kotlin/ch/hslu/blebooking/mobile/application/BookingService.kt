
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.model.Booking

internal class BookingService(
    private val apiClient: ApiClient
) {

    suspend fun getActiveBookings(): List<Booking> {
        TODO("Not yet implemented")
    }
}
