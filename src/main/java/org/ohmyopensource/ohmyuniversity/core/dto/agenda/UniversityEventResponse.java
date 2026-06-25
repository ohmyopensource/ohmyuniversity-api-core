package org.ohmyopensource.ohmyuniversity.core.dto.agenda;

import org.ohmyopensource.ohmyuniversity.core.domain.entity.CalendarEventType;

/**
 * Response DTO for university-published event endpoints.
 *
 * <p>Returned by {@code GET /api/v1/calendar/university-events}. The {@code imported} flag
 * indicates whether the authenticated student has already added this event to their personal
 * calendar. Date and time fields are serialised as ISO-8601 strings (UTC).
 */
public class UniversityEventResponse {

  private String id;

  /** Identifier of the university that published this event (e.g. {@code UNIMOL}). */
  private String universityId;
  private String title;
  private String description;

  /** Event start timestamp as an ISO-8601 string (UTC). */
  private String startDate;

  /** Optional event end timestamp as an ISO-8601 string (UTC). */
  private String endDate;
  private boolean allDay;
  private CalendarEventType type;
  private String color;
  private String url;
  private String sourceUrl;

  /** Timestamp when the event was published into the system, as an ISO-8601 string (UTC). */
  private String publishedAt;

  /**
   * Whether the authenticated student has already imported this event into their personal
   * calendar.
   */
  private boolean imported;

  // ============ Getters | Setters | Bool ============

  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  public String getUniversityId() {
    return universityId;
  }
  public void setUniversityId(String universityId) {
    this.universityId = universityId;
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

  public String getSourceUrl() {
    return sourceUrl;
  }
  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public String getPublishedAt() {
    return publishedAt;
  }
  public void setPublishedAt(String publishedAt) {
    this.publishedAt = publishedAt;
  }

  public boolean isImported() {
    return imported;
  }
  public void setImported(boolean imported) {
    this.imported = imported;
  }
}