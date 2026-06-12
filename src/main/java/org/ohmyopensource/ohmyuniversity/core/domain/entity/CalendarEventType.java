package org.ohmyopensource.ohmyuniversity.core.domain.entity;

/**
 * Classification of a calendar event.
 *
 * <p>Used by both {@link CalendarEvent} and {@link UniversityEvent} to drive frontend icon and
 * colour rendering. Persisted as a {@code varchar(20)} column — values are validated by this enum
 * at the application layer.
 */
public enum CalendarEventType {

  /** Generic personal event created by the student. */
  PERSONAL,

  /** Exam session. */
  EXAM,

  /** Administrative or academic deadline. */
  DEADLINE,

  /** Personal reminder. */
  REMINDER,

  /** University-published event, optionally imported into the student's personal calendar. */
  UNIVERSITY
}