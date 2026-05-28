package org.ohmyopensource.ohmyuniversity.core.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core identity entity representing a stable OhMyUniversity user.
 *
 * <p>This entity is NOT a replica of Cineca or university-specific data. It represents a unified
 * person identity across multiple universities.
 *
 * <p>The main identifier is the Italian fiscal code (codice fiscale), which acts as the
 * cross-university linking key.
 *
 * <p>Responsibilities:
 * - Provide a stable internal user identity
 * - Link multiple UniversityConnection records
 * - Track authentication metadata (creation, last login)
 *
 * <p>No academic or career data is stored in this entity. All student-related information is
 * retrieved dynamically from Cineca APIs and optionally cached externally (e.g., Redis).
 */
@Entity
@Table(name = "omu_user")
public class OmuUser {

  /**
   * Unique internal identifier (UUID).
   */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  /**
   * Italian fiscal code used as cross-university unique identifier.
   */
  @NotBlank
  @Column(name = "codice_fiscale", nullable = false, unique = true, length = 16)
  private String codiceFiscale;

  /**
   * Primary email address of the user in the OhMyUniversity system.
   */
  @Column(name = "email_primaria")
  private String emailPrimaria;

  /**
   * Timestamp of user creation.
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Timestamp of last successful login.
   */
  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  /**
   * List of university-specific connections associated with this user.
   *
   * <p>Represents the relationship between the global user identity and multiple Cineca/university
   * accounts.
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
      fetch = FetchType.LAZY, orphanRemoval = true)
  private List<UniversityConnection> universityConnections = new ArrayList<>();

  // ============ Class Methods ============

  /**
   * Automatically sets creation timestamp before persistence.
   */
  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

  // ============ Getters | Setters | Bool ============

  public UUID getId() {
    return id;
  }

  public String getCodiceFiscale() {
    return codiceFiscale;
  }

  public void setCodiceFiscale(String codiceFiscale) {
    this.codiceFiscale = codiceFiscale;
  }

  public String getEmailPrimaria() {
    return emailPrimaria;
  }

  public void setEmailPrimaria(String emailPrimaria) {
    this.emailPrimaria = emailPrimaria;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public List<UniversityConnection> getUniversityConnections() {
    return universityConnections;
  }
}