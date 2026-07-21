package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity that links an OhMyUniversity user to a specific platform vendor account (Cineca/ESSE3,
 * GOMP, Multiversity, ...).
 *
 * <p>A single user can have multiple university connections, each representing:
 * - a different university tenant - or multiple accounts within the same university context
 *
 * <p>This entity does NOT store any authentication/session credentials.
 * Vendor tokens (JWT, authToken) are stored in Redis with TTL and never persisted here — the auth
 * service's AuthSessionStore owns that data.
 *
 * <p>This is a local shadow copy: the auth service is the source of truth for
 * this data, writing to its own database. The core service maintains this mirror only to satisfy
 * its own foreign key constraints (e.g. on cached_profilo_carriera), kept in sync via the
 * user.authenticated Kafka event consumed by {@code UserAuthenticatedEventListener}.
 *
 * <p>Responsibilities:
 * - Map internal user identity to an external vendor account - Store university metadata (id, name,
 * base URL) - Track connection lifecycle (creation, last usage)
 */
@Entity
@Table(name = "university_connection")
public class UniversityConnection {

  /**
   * Unique identifier of the university connection.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  /**
   * Owning OhMyUniversity user associated with this connection.
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private OmuUser user;

  /**
   * University tenant identifier (e.g. UNIMOL, POLIMI, UNIROMA1).
   */
  @NotBlank
  @Column(name = "university_id", nullable = false, length = 20)
  private String universityId;

  /**
   * Human-readable university name.
   */
  @NotBlank
  @Column(name = "university_name", nullable = false)
  private String universityName;

  /**
   * Base URL of the platform vendor's API for this university instance (e.g. Cineca ESSE3 REST API
   * base URL).
   */
  @NotBlank
  @Column(name = "vendor_base_url", nullable = false, length = 500)
  private String vendorBaseUrl;

  /**
   * Username used to authenticate against the vendor system.
   */
  @NotBlank
  @Column(name = "username_vendor", nullable = false)
  private String usernameVendor;

  /**
   * Timestamp when the connection was first created.
   */
  @Column(name = "connected_at", nullable = false, updatable = false)
  private Instant connectedAt;

  /**
   * Timestamp of last usage of this connection.
   */
  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  // ============ Class Methods ============

  /**
   * Automatically sets creation timestamp before persisting the entity.
   */
  @PrePersist
  void onCreate() {
    connectedAt = Instant.now();
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

  public String getUniversityName() {
    return universityName;
  }

  public void setUniversityName(String universityName) {
    this.universityName = universityName;
  }

  public String getVendorBaseUrl() {
    return vendorBaseUrl;
  }

  public void setVendorBaseUrl(String vendorBaseUrl) {
    this.vendorBaseUrl = vendorBaseUrl;
  }

  public String getUsernameVendor() {
    return usernameVendor;
  }

  public void setUsernameVendor(String usernameVendor) {
    this.usernameVendor = usernameVendor;
  }

  public Instant getConnectedAt() {
    return connectedAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}