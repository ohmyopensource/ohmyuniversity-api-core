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
 * Entity that links an OhMyUniversity user to a specific Cineca/ESSE3 account.
 *
 * <p>A single user can have multiple university connections, each representing:
 * - a different university tenant
 * - or multiple Cineca accounts within the same university context
 *
 * <p>This entity does NOT store any authentication/session credentials.
 * Cineca tokens (JWT, authToken) are stored in Redis with TTL and never persisted.
 *
 * <p>Responsibilities:
 * - Map internal user identity to external Cineca account
 * - Store university metadata (id, name, base URL)
 * - Track connection lifecycle (creation, last usage)
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
   * University tenant identifier used in Cineca context.
   *
   * <p>Example: UNIMOL, POLIMI, UNIROMA1
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
   * Base URL of the Cineca ESSE3 API for this university instance.
   */
  @NotBlank
  @Column(name = "cineca_base_url", nullable = false, length = 500)
  private String cinecaBaseUrl;

  /**
   * Cineca username used to authenticate against the ESSE3 system.
   */
  @NotBlank
  @Column(name = "username_cineca", nullable = false)
  private String usernameCineca;

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

  public String getCinecaBaseUrl() {
    return cinecaBaseUrl;
  }

  public void setCinecaBaseUrl(String cinecaBaseUrl) {
    this.cinecaBaseUrl = cinecaBaseUrl;
  }

  public String getUsernameCineca() {
    return usernameCineca;
  }

  public void setUsernameCineca(String usernameCineca) {
    this.usernameCineca = usernameCineca;
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