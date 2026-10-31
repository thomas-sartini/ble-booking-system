/**
 * Account activation, login, access and refresh tokens, password reset.
 */
@ApplicationModule(allowedDependencies = { "common", "user" })
package ch.hslu.blebooking.backend.auth;

import org.springframework.modulith.ApplicationModule;
