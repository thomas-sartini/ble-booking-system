package ch.hslu.blebooking.backend.notification.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link MailProperties}.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
class MailConfig {
}
