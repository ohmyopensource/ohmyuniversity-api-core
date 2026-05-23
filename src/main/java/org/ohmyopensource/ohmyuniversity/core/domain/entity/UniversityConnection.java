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
 * Links an OhMyUniversity user to a specific Cineca/ESSE3 university account.
 *
 * One user can have multiple connections — different universities, or different
 * usernames at the same university (e.g. separate accounts for two degree courses).
 *
 * No Cineca tokens are stored here. Session tokens (authToken, JWT) live in
 * Redis with TTL only, never persisted to the database.
 */
@Entity
@Table(name = "university_connection")
public class UniversityConnection {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private OmuUser user;

  /**
   * Short university identifier matching the Cineca JWT "tenant" field.
   * Examples: "UNIMOL", "UNIROMA1", "POLIMI".
   */
  @NotBlank
  @Column(name = "university_id", nullable = false, length = 20)
  private String universityId;

  @NotBlank
  @Column(name = "university_name", nullable = false)
  private String universityName;

  /**
   * Base URL of the ESSE3 REST API for this university.
   * Example: "https://unimol.esse3.cineca.it/e3rest/api"
   */
  @NotBlank
  @Column(name = "cineca_base_url", nullable = false, length = 500)
  private String cinecaBaseUrl;

  /**
   * Username used to authenticate against this university ESSE3 instance.
   * Example: "a.delmuto"
   */
  @NotBlank
  @Column(name = "username_cineca", nullable = false)
  private String usernameCineca;

  @Column(name = "connected_at", nullable = false, updatable = false)
  private Instant connectedAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @PrePersist
  void onCreate() {
    connectedAt = Instant.now();
  }

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