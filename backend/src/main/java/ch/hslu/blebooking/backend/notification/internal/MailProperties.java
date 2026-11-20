package ch.hslu.blebooking.backend.notification.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mail settings of the application. The SMTP server itself is configured with spring.mail.*.
 *
 * @param enabled whether booking emails are sent at all (APP_MAIL_ENABLED, default false)
 * @param from    sender address (APP_MAIL_FROM)
 */
@ConfigurationProperties("app.mail")
record MailProperties(boolean enabled, String from) {
}
