package org.ohmyopensource.ohmyuniversity.core.dto.calendar;

import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventType;

/**
 * Response DTO for personal calendar event endpoints.
 *
 * <p>Returned by {@code GET}, {@code POST}, and {@code PUT} {@code /api/v1/calendar/events}.
 * Date and time fields are serialised as ISO-8601 strings (UTC).
 */
public class CalendarEventResponse {

  private String id;
  private String title;
  private String description;
  private String startDate;
  private String endDate;
  private boolean allDay;
  private CalendarEventType type;
  private String color;
  private String url;
  private String notes;
  private String createdAt;
  private String updatedAt;

  // ============ Getters | Setters | Bool ============

  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

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

  public String getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }
  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}