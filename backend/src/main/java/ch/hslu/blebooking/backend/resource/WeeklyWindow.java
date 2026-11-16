package ch.hslu.blebooking.backend.resource;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * A weekly bookable window in local time of the booking zone, e.g. MONDAY 08:00-18:00.
 */
public record WeeklyWindow(
		DayOfWeek dayOfWeek,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime start,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime end) {
}
