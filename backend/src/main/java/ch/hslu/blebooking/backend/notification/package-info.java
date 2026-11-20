/**
 * Email notifications for bookings (docs/api/booking-api.md §6). Listens to the booking events after commit
 * and loads what it needs through the public APIs of the other modules.
 */
@ApplicationModule(allowedDependencies = { "booking", "user", "resource", "common" })
package ch.hslu.blebooking.backend.notification;

import org.springframework.modulith.ApplicationModule;
