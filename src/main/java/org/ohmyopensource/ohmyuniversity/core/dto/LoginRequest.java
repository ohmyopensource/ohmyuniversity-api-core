package org.ohmyopensource.ohmyuniversity.core.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/auth/login.
 *
 * This object represents the credentials required to authenticate
 * a student against a Cineca ESSE3 university instance.
 *
 * Authentication is delegated to the external university system.
 * No credentials are persisted or stored; only the resulting session
 * (JWT / auth token) is cached in Redis after successful login.
 *
 * All fields are required and validated to prevent incomplete
 * authentication requests from reaching the service layer.
 */
public class LoginRequest {

  /**
   * University identifier (e.g. UNIMOL, POLIMI).
   * Must match a configured entry in UniversityRegistry.
   */
  @NotBlank
  private String universityId;

  /**
   * Cineca username used for ESSE3 authentication.
   */
  @NotBlank
  private String username;

  /**
   * Cineca password.
   *
   * Transmitted only over HTTPS and never persisted or logged.
   */
  @NotBlank
  private String password;

  // ============ Getters | Setters | Bool ============

  public String getUniversityId() { return universityId; }
  public void setUniversityId(String universityId) { this.universityId = universityId; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
}