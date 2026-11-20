package ch.hslu.blebooking.backend.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link BookingProperties}.
 */
@Configuration
@EnableConfigurationProperties(BookingProperties.class)
class BookingConfig {
}
