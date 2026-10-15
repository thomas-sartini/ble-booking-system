
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.model.Booking

internal class BookingService(
    private val communicationGateway: CommunicationGateway
) {

    suspend fun getActiveBookings(): List<Booking> {
        TODO("Not yet implemented")
    }
}
