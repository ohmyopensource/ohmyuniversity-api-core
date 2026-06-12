package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * University-published event visible to all students of a given university.
 *
 * <p>These events are created by university staff or ingested automatically by the fetcher service
 * (e.g. from the university news feed). Students can import them into their personal calendar via
 * {@link CalendarEventImport}.
 */
@Entity
@Table(name = "university_event")
public class UniversityEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "university_id", nullable = false, length = 50)
  private String universityId;

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
  private CalendarEventType type = CalendarEventType.UNIVERSITY;

  @Column(length = 7)
  private String color;

  @Column(length = 2048)
  private String url;

  @Column(name = "source_url", length = 2048)
  private String sourceUrl;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt = Instant.now();

  // ============ Getters | Setters | Bool ============

  public UUID getId() {
    return id;
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

  public String getSourceUrl() {
    return sourceUrl;
  }
  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }
  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
  }
}