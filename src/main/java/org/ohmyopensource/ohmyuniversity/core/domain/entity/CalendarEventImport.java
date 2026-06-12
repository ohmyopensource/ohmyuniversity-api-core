package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Bridge entity tracking which university events a student has imported into their personal
 * calendar.
 *
 * <p>The unique constraint on {@code (omu_user_id, university_event_id)} ensures a student cannot
 * import the same university event more than once.
 */
@Entity
@Table(
    name = "calendar_event_import",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"omu_user_id", "university_event_id"}))
public class CalendarEventImport {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "omu_user_id", nullable = false)
  private OmuUser user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "university_event_id", nullable = false)
  private UniversityEvent universityEvent;

  @Column(name = "imported_at", nullable = false)
  private final Instant importedAt = Instant.now();

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

  public UniversityEvent getUniversityEvent() {
    return universityEvent;
  }
  public void setUniversityEvent(UniversityEvent universityEvent) {
    this.universityEvent = universityEvent;
  }

  public Instant getImportedAt() {
    return importedAt;
  }
}