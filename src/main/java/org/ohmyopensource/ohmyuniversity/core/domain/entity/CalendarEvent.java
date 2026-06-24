package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Personal calendar event owned by an OhMyUniversity student.
 *
 * <p>Each event belongs to a single {@link OmuUser} regardless of university — the calendar is
 * shared across all the student's university enrollments.
 *
 * <p>Optional fields: {@code description}, {@code endDate}, {@code color}, {@code url},
 * {@code notes}. When {@code allDay} is {@code true}, the time component of {@code startDate}
 * and {@code endDate} is ignored by the client.
 */
@Entity
@Table(name = "calendar_event")
public class CalendarEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "omu_user_id", nullable = false)
  private OmuUser user;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "start_date", nullable = false)
  private Instant startDate;

  @Column(name = "end_date")
  private Instant endDate;

  @Column(name = "all_day", nullable = false)
  private boolean allDay = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CalendarEventType type = CalendarEventType.PERSONAL;

  @Column(length = 7)
  private String color;

  @Column(length = 2048)
  private String url;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(name = "location", length = 500)
  private String location;

  @Column(name = "created_at", nullable = false, updatable = false)
  private final Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  // ============ Getters | Setters | Bool ============

  public UUID getId() {
    return id;
  }

  public OmuUser getUser() {
    return user;
  }
  public void setUser(OmuUser user) {
    this.user = user;
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

  public Instant getStartDate() {
    return startDate;
  }
  public void setStartDate(Instant startDate) {
    this.startDate = startDate;
  }

  public Instant getEndDate() {
    return endDate;
  }
  public void setEndDate(Instant endDate) {
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

  public Instant getCreatedAt() {
    return createdAt;
  }
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}