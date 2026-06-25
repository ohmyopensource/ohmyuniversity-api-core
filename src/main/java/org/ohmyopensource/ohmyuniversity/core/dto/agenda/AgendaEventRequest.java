package org.ohmyopensource.ohmyuniversity.core.dto.agenda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventType;

/**
 * Request DTO for {@code POST} and {@code PUT} {@code /api/v1/calendar/events}.
 *
 * <p>{@code title} and {@code startDate} are required. All other fields are optional and default
 * to {@code null} unless specified. {@code type} defaults to {@link CalendarEventType#PERSONAL}
 * when omitted.
 */
public class AgendaEventRequest {

  @NotBlank
  private String title;

  private String description;

  /** Event start timestamp in ISO-8601 format (e.g. {@code 2026-07-15T09:00:00Z}). Required. */
  @NotNull
  private String startDate;

  private String endDate;

  /** Whether the event spans an entire day. Defaults to {@code false}. */
  private boolean allDay = false;

  /** Event classification. Defaults to {@link CalendarEventType#PERSONAL} when omitted. */
  private CalendarEventType type = CalendarEventType.PERSONAL;

  private String color;

  private String url;

  private String notes;

  private String location;

  // ============ Getters | Setters | Bool ============

  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  public String getStartDate() {
    return startDate;
  }
  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public String getEndDate() {
    return endDate;
  }
  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public boolean isAllDay() {
    return allDay;
  }
  public void setAllDay(boolean allDay) {
    this.allDay = allDay;
  }

  public CalendarEventType getType() {
    return type;
  }
  public void setType(CalendarEventType type) {
    this.type = type;
  }

  public String getColor() {
    return color;
  }
  public void setColor(String color) {
    this.color = color;
  }

  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }

  public String getNotes() {
    return notes;
  }
  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }
}