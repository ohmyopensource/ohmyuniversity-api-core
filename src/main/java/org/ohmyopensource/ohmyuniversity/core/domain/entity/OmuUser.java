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
 * Stable OhMyUniversity identity for a person.
 *
 * This is NOT a copy of Cineca student data. It is the aggregating identity
 * that links one physical person across multiple universities and careers.
 * The codiceFiscale is the natural cross-university key — the same person
 * always has the same Italian tax code regardless of which university they attend.
 *
 * No academic data is stored here. All student data (grades, exams, career
 * segments) is fetched from Cineca on demand and cached in Redis with TTL.
 */
@Entity
@Table(name = "omu_user")
public class OmuUser {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
  private UUID id;

  /**
   * Italian tax code — natural cross-university identifier.
   * Used to match the same person across different university accounts.
   */
  @NotBlank
  @Column(name = "codice_fiscale", nullable = false, unique = true, length = 16)
  private String codiceFiscale;

  /**
   * Primary contact email registered on OhMyUniversity.
   * May differ from university institutional emails.
   */
  @Column(name = "email_primaria")
  private String emailPrimaria;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
      fetch = FetchType.LAZY, orphanRemoval = true)
  private List<UniversityConnection> universityConnections = new ArrayList<>();

  @PrePersist
  void onCreate() {
    createdAt = Instant.now();
  }

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