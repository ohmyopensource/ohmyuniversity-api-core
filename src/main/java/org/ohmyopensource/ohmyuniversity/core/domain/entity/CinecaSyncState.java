package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity tracking which Kafka integration events have already been published
 * for a given user/university/course combination.
 *
 * <p>Acts as a deduplication guard for the Cineca sync process.
 * Each row means "we already notified downstream services about this fact".
 *
 * <p>This entity does NOT store academic or career data.
 * It only tracks notification state for Kafka event publishing.
 */
@Entity
@Table(
    name = "cineca_sync_state",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_cineca_sync_state",
        columnNames = {"user_id", "university_id", "adsce_id", "event_type"}
    )
)
public class CinecaSyncState {

  /**
   * Supported Kafka event types tracked by this entity.
   */
  public enum EventType {
    COURSE_EDITION,
    ENROLLMENT,
    TEACHING_ASSIGNMENT,
    CAMPUS_ASSIGNMENT
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private OmuUser user;

  @NotBlank
  @Column(name = "university_id", nullable = false, length = 20)
  private String universityId;

  @NotNull
  @Column(name = "adsce_id", nullable = false)
  private Long adsceId;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 50)
  private EventType eventType;

  @Column(name = "external_channel_id", length = 255)
  private String externalChannelId;

  @Column(name = "notified_at", nullable = false, updatable = false)
  private Instant notifiedAt;

  // ============ Class Methods ============

  /**
   * Automatically sets notification timestamp before persisting.
   */
  @PrePersist
  void onCreate() {
    notifiedAt = Instant.now();
  }

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

  public String getUniversityId() {
    return universityId;
  }

  public void setUniversityId(String universityId) {
    this.universityId = universityId;
  }

  public Long getAdsceId() {
    return adsceId;
  }

  public void setAdsceId(Long adsceId) {
    this.adsceId = adsceId;
  }

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public String getExternalChannelId() {
    return externalChannelId;
  }

  public void setExternalChannelId(String externalChannelId) {
    this.externalChannelId = externalChannelId;
  }

  public Instant getNotifiedAt() {
    return notifiedAt;
  }
}