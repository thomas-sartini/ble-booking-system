/**
 * Invoices with Swiss QR bill, created when a booking ends.
 */
@ApplicationModule(allowedDependencies = { "common", "booking", "user", "resource" })
package ch.hslu.blebooking.backend.invoice;

import org.springframework.modulith.ApplicationModule;
