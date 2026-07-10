package org.ohmyopensource.ohmyuniversity.core.dto;

import java.time.Instant;

/**
 * API representation of a tracked OhMyUniversity user session (device/IP metadata), returned by GET
 * /api/v1/auth/sessions. Not to be confused with dto.esse3.SessionsResponse, which represents
 * Cineca exam booking sessions — an unrelated domain.
 */
public class AuthSessionResponse {

  private String sessionId;
  private String ipAddress;
  private String userAgent;
  private Instant createdAt;
  private Instant lastUsedAt;
  private boolean current;

  public AuthSessionResponse() {
  }

  public AuthSessionResponse(String sessionId, String ipAddress, String userAgent,
      Instant createdAt, Instant lastUsedAt, boolean current) {
    this.sessionId = sessionId;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.createdAt = createdAt;
    this.lastUsedAt = lastUsedAt;
    this.current = current;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public boolean isCurrent() {
    return current;
  }

  public void setCurrent(boolean current) {
    this.current = current;
  }
}